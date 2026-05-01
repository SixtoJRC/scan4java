package com.sijareca.scan4java.internal.twain;

import com.sijareca.scan4java.ScanException;
import com.sun.jna.Memory;
import com.sun.jna.Native;
//import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;

import java.util.ArrayList;
import java.util.List;

class TwainSession implements AutoCloseable {

    private final TwainLib             lib;
    private final TwainLib.TW_IDENTITY appId;
    private       boolean              open = false;

    TwainSession(TwainLib lib) throws ScanException {
        this.lib   = lib;
        this.appId = buildAppIdentity();
        open();
    }

    private TwainLib.TW_IDENTITY buildAppIdentity() {
        TwainLib.TW_IDENTITY id = new TwainLib.TW_IDENTITY();
        id.Version.MajorNum  = 1;
        id.Version.MinorNum  = 0;
        id.ProtocolMajor     = 2;
        id.ProtocolMinor     = 3;
        id.SupportedGroups   = 2; // DG_IMAGE | DG_CONTROL
        copyString(id.Manufacturer,  "sijareca");
        copyString(id.ProductFamily, "scan4java");
        copyString(id.ProductName,   "scan4java");
        return id;
    }

    private void open() throws ScanException {
        // TWAIN necesita un HWND — usamos el escritorio como ventana padre
        WinDef.HWND desktopHwnd = User32.INSTANCE.GetDesktopWindow();

        // Empaquetamos el HWND como Pointer dentro de un bloque de memoria
        // DAT_PARENT espera un TW_MEMREF que apunta a un HWND (puntero a puntero)
        Memory hwndMem = new Memory(Native.POINTER_SIZE);
        hwndMem.setPointer(0, desktopHwnd.getPointer());

        int rc = lib.DSM_Entry(appId, null,
                               TwainLib.DG_CONTROL,
                               TwainLib.DAT_PARENT,
                               TwainLib.MSG_OPENDSM,
                               hwndMem);

        if (rc != TwainLib.TWRC_SUCCESS) {
            throw new ScanException("Cannot open TWAIN DSM (rc=" + rc + ")");
        }
        open = true;
    }

    List<TwainLib.TW_IDENTITY> enumerateSources() throws ScanException {
        List<TwainLib.TW_IDENTITY> sources = new ArrayList<>();
        TwainLib.TW_IDENTITY ds = new TwainLib.TW_IDENTITY();

        int rc = lib.DSM_Entry(appId, null,
                               TwainLib.DG_CONTROL,
                               TwainLib.DAT_IDENTITY,
                               TwainLib.MSG_GETFIRST,
                               ds);

        while (rc == TwainLib.TWRC_SUCCESS) {
            sources.add(ds);
            ds = new TwainLib.TW_IDENTITY();
            rc = lib.DSM_Entry(appId, null,
                               TwainLib.DG_CONTROL,
                               TwainLib.DAT_IDENTITY,
                               TwainLib.MSG_GETNEXT,
                               ds);
        }

        if (rc != TwainLib.TWRC_ENDOFLIST) {
            throw new ScanException("Error enumerating TWAIN sources (rc=" + rc + ")");
        }

        return sources;
    }

    TwainLib             getLib()   { return lib;   }
    TwainLib.TW_IDENTITY getAppId() { return appId; }

    @Override
    public void close() {
        if (!open) return;
        // DAT_PARENT / MSG_CLOSEDSM también necesita el Pointer al HWND
        WinDef.HWND desktopHwnd = User32.INSTANCE.GetDesktopWindow();
        Memory hwndMem = new Memory(Native.POINTER_SIZE);
        hwndMem.setPointer(0, desktopHwnd.getPointer());
        lib.DSM_Entry(appId, null,
                      TwainLib.DG_CONTROL,
                      TwainLib.DAT_PARENT,
                      TwainLib.MSG_CLOSEDSM,
                      hwndMem);
        open = false;
    }

    private static void copyString(byte[] target, String value) {
        byte[] bytes = value.getBytes();
        System.arraycopy(bytes, 0, target, 0,
                         Math.min(bytes.length, target.length - 1));
    }
}