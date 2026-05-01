package com.sijareca.scan4java.internal.twain;

import com.sijareca.scan4java.*;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class TwainScanner implements Scanner {

    private static final System.Logger logger =
        System.getLogger(TwainScanner.class.getName());

    private final String   id;
    private final String   name;
    private       ScanConfig config = ScanConfig.defaults();

    TwainScanner(String id, String name) {
        this.id   = id;
        this.name = name;
    }

    @Override public String   getId()        { return id;             }
    @Override public String   getName()      { return name;           }
    @Override public Protocol getProtocol()  { return Protocol.TWAIN; }

    @Override public Scanner setDpi(int dpi)          { config.dpi(dpi);       return this; }
    @Override public Scanner setColor(ColorMode mode) { config.color(mode);    return this; }
    @Override public Scanner setAdf(boolean adf)      { config.adf(adf);       return this; }
    @Override public Scanner setDuplex(boolean dup)   { config.duplex(dup);    return this; }

    @Override
    public List<BufferedImage> scan() throws ScanException {
        return scan(config);
    }

    @Override
    public List<BufferedImage> scan(ScanConfig config) throws ScanException {
        TwainLib lib = TwainLib.load();
        List<BufferedImage> pages = new ArrayList<>();

        // Creamos ventana oculta — TWAIN necesita un HWND para el bucle de mensajes
        WinDef.HWND hwnd = User32.INSTANCE.CreateWindowEx(
            0, "STATIC", "", 0x80000000,
            0, 0, 0, 0,
            User32.INSTANCE.GetDesktopWindow(),
            null, null, null);

        if (hwnd == null) {
            throw new ScanException("Cannot create TWAIN window");
        }

        try {
            TwainLib.TW_IDENTITY appId = TwainUtils.buildAppIdentity();

            // Estado 2→3: abrir DSM
            Memory hwndMem = new Memory(Native.POINTER_SIZE);
            hwndMem.setPointer(0, hwnd.getPointer());
            int rc = lib.DSM_Entry(appId, null,
                TwainLib.DG_CONTROL, TwainLib.DAT_PARENT,
                TwainLib.MSG_OPENDSM, hwndMem);
            if (rc != TwainLib.TWRC_SUCCESS) {
                throw new ScanException("Cannot open DSM (rc=" + rc + ")");
            }

            try {
                // Estado 3→4: abrir DS por nombre
                TwainLib.TW_IDENTITY dsId = findSource(lib, appId);

                rc = lib.DSM_Entry(appId, null,
                    TwainLib.DG_CONTROL, TwainLib.DAT_IDENTITY,
                    TwainLib.MSG_OPENDS, dsId);
                if (rc != TwainLib.TWRC_SUCCESS) {
                    throw new ScanException("Cannot open DS '" + name + "' (rc=" + rc + ")");
                }

                try {
                    // Estado 4→5: habilitar DS (muestra UI del escáner)
                    TwainLib.TW_USERINTERFACE ui = new TwainLib.TW_USERINTERFACE();
                    ui.ShowUI   = 1;
                    ui.ModalUI  = 0;
                    ui.hParent  = hwnd.getPointer();

                    rc = lib.DSM_Entry(appId, dsId,
                        TwainLib.DG_CONTROL, TwainLib.DAT_USERINTERFACE,
                        TwainLib.MSG_ENABLEDS, ui);
                    if (rc != TwainLib.TWRC_SUCCESS) {
                        throw new ScanException("Cannot enable DS (rc=" + rc + ")");
                    }

                    // Bucle de mensajes Windows — TWAIN envía eventos via WM_USER
                    User32.MSG msg = new User32.MSG();
                    TwainLib.TW_EVENT twEvent = new TwainLib.TW_EVENT();

                    outer:
                    while (User32.INSTANCE.GetMessage(msg, null, 0, 0) > 0) {
                        twEvent.pEvent   = msg.getPointer();
                        twEvent.TWMessage = 0;
                        twEvent.write();

                        rc = lib.DSM_Entry(appId, dsId,
                            TwainLib.DG_CONTROL, TwainLib.DAT_EVENT,
                            TwainLib.MSG_PROCESSEVENT, twEvent);

                        if (rc == TwainLib.TWRC_NOTDSEVENT) {
                            User32.INSTANCE.TranslateMessage(msg);
                            User32.INSTANCE.DispatchMessage(msg);
                            continue;
                        }

                        switch (twEvent.TWMessage) {
                            case TwainLib.MSG_CLOSEDSREQ -> { break outer; }
                            case TwainLib.MSG_XFERREADY  -> {
                                transferPages(lib, appId, dsId, pages);
                                break outer;
                            }
                        }
                    }

                    // Estado 5→4: deshabilitar DS
                    int rcDisable = lib.DSM_Entry(appId, dsId,
                        TwainLib.DG_CONTROL, TwainLib.DAT_USERINTERFACE,
                        TwainLib.MSG_DISABLEDS, ui);
                    if (rcDisable != TwainLib.TWRC_SUCCESS)
                        logger.log(System.Logger.Level.WARNING,
                            "MSG_DISABLEDS failed (rc={0})", rcDisable);

                } finally {
                    // Estado 4→3: cerrar DS
                    lib.DSM_Entry(appId, null,
                        TwainLib.DG_CONTROL, TwainLib.DAT_IDENTITY,
                        TwainLib.MSG_CLOSEDS, dsId);
                }
            } finally {
                // Estado 3→2: cerrar DSM
                lib.DSM_Entry(appId, null,
                    TwainLib.DG_CONTROL, TwainLib.DAT_PARENT,
                    TwainLib.MSG_CLOSEDSM, hwndMem);
            }
        } finally {
            User32.INSTANCE.DestroyWindow(hwnd);
        }

        return pages;
    }

    // Busca el DS por nombre entre los disponibles
    private TwainLib.TW_IDENTITY findSource(TwainLib lib,
                                            TwainLib.TW_IDENTITY appId)
                                            throws ScanException {
        TwainLib.TW_IDENTITY ds = new TwainLib.TW_IDENTITY();
        int rc = lib.DSM_Entry(appId, null,
            TwainLib.DG_CONTROL, TwainLib.DAT_IDENTITY,
            TwainLib.MSG_GETFIRST, ds);

        while (rc == TwainLib.TWRC_SUCCESS) {
            if (ds.getProductName().equalsIgnoreCase(name)) return ds;
            ds = new TwainLib.TW_IDENTITY();
            rc = lib.DSM_Entry(appId, null,
                TwainLib.DG_CONTROL, TwainLib.DAT_IDENTITY,
                TwainLib.MSG_GETNEXT, ds);
        }
        throw new ScanException("TWAIN source not found: " + name);
    }

    // Transfiere todas las páginas pendientes (ADF)
    private void transferPages(TwainLib lib,
                               TwainLib.TW_IDENTITY appId,
                               TwainLib.TW_IDENTITY dsId,
                               List<BufferedImage> pages) throws ScanException {
        TwainLib.TW_PENDINGXFERS pxfers = new TwainLib.TW_PENDINGXFERS();

        do {
            int[] handle = new int[1];
            int rc = lib.DSM_Entry(appId, dsId,
                TwainLib.DG_IMAGE, TwainLib.DAT_IMAGENATIVEXFER,
                TwainLib.MSG_GET, handle);

            if (rc != TwainLib.TWRC_XFERDONE) {
                // Reset y salir — el usuario canceló o error
                lib.DSM_Entry(appId, dsId,
                    TwainLib.DG_CONTROL, TwainLib.DAT_PENDINGXFERS,
                    TwainLib.MSG_RESET, pxfers);
                return;
            }

            // El handle es un HGLOBAL — lo tratamos como Pointer directamente
            Pointer hGlobal = Pointer.createConstant(handle[0]);

            Pointer p = Kernel32Heap.INSTANCE.GlobalLock(hGlobal);
            try {
                if (p != null) {
                    pages.add(DibConverter.toBufferedImage(p));
                }
            } finally {
                Kernel32Heap.INSTANCE.GlobalUnlock(hGlobal);
                Kernel32Heap.INSTANCE.GlobalFree(hGlobal);
            }

            // EndXfer — pregunta si hay más páginas (ADF)
            rc = lib.DSM_Entry(appId, dsId,
                TwainLib.DG_CONTROL, TwainLib.DAT_PENDINGXFERS,
                TwainLib.MSG_ENDXFER, pxfers);

        } while (pxfers.Count > 0);

        // Reset final
        lib.DSM_Entry(appId, dsId,
            TwainLib.DG_CONTROL, TwainLib.DAT_PENDINGXFERS,
            TwainLib.MSG_RESET, pxfers);
    }

}