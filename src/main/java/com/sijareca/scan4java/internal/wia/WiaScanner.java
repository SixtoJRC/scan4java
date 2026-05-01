package com.sijareca.scan4java.internal.wia;

import com.sijareca.scan4java.*;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.ptr.PointerByReference;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class WiaScanner implements Scanner {

    private final String     id;
    private final String     name;
    private       ScanConfig config = ScanConfig.defaults();

    WiaScanner(String id, String name) {
        this.id   = id;
        this.name = name;
    }

    @Override public String   getId()        { return id;           }
    @Override public String   getName()      { return name;         }
    @Override public Protocol getProtocol()  { return Protocol.WIA; }

    @Override public Scanner setDpi(int dpi)          { config.dpi(dpi);    return this; }
    @Override public Scanner setColor(ColorMode mode) { config.color(mode); return this; }
    @Override public Scanner setAdf(boolean adf)      { config.adf(adf);    return this; }
    @Override public Scanner setDuplex(boolean dup)   { config.duplex(dup); return this; }

    @Override
    public List<BufferedImage> scan() throws ScanException {
        return scan(config);
    }

    @Override
    public List<BufferedImage> scan(ScanConfig config) throws ScanException {
        List<BufferedImage> pages = new ArrayList<>();

        try (WiaSession session = new WiaSession()) {
            // Abrir dispositivo
            WiaLib.IWiaItem device = session.openDevice(id);
            try {
                // Configurar propiedades de escaneo
                configureDevice(device, config);

                // Obtener el primer item hijo (el area de escaneo)
                WiaLib.IWiaItem scanItem = getFirstChildItem(device);
                if (scanItem == null) {
                    throw new ScanException("No scan items found for device: " + name);
                }
                try {
                    // Configurar formato de salida en el item
                    configureItem(scanItem, config);

                    // Transferir a fichero temporal BMP
                    File tempFile = transferToFile(scanItem);
                    try {
                        BufferedImage img = ImageIO.read(tempFile);
                        if (img != null) pages.add(img);
                    } catch (IOException e) {
                        throw new ScanException("Cannot read scanned image", e);
                    } finally {
                        tempFile.delete();
                    }
                } finally {
                    scanItem.Release();
                }
            } finally {
                device.Release();
            }
        }
        return pages;
    }

    private void configureDevice(WiaLib.IWiaItem device,
                                ScanConfig config) throws ScanException {
        PointerByReference pProps = new PointerByReference();
        HRESULT hr = device.queryPropertyStorage(pProps);
        if (hr.intValue() != WiaLib.S_OK) return;

        WiaLib.IWiaPropertyStorage props =
            new WiaLib.IWiaPropertyStorage(pProps.getValue());
        try {
            writeIntProperty(props, 6147, config.getDpi());
            writeIntProperty(props, 6148, config.getDpi());
            writeIntProperty(props, 4103,
                config.getColor() == ColorMode.COLOR ? 3 :
                config.getColor() == ColorMode.GRAYSCALE ? 2 : 0);
        } finally {
            props.Release();
        }
    }

    private void configureItem(WiaLib.IWiaItem item,
                            ScanConfig config) throws ScanException {
        PointerByReference pProps = new PointerByReference();
        HRESULT hr = item.queryPropertyStorage(pProps);
        if (hr.intValue() != WiaLib.S_OK) return;

        WiaLib.IWiaPropertyStorage props =
            new WiaLib.IWiaPropertyStorage(pProps.getValue());
        try {
            writeIntProperty(props, WiaLib.WIA_IPA_TYMED, WiaLib.TYMED_FILE);
        } finally {
            props.Release();
        }
    }

    private WiaLib.IWiaItem getFirstChildItem(WiaLib.IWiaItem device)
                                              throws ScanException {
        PointerByReference pEnum = new PointerByReference();
        HRESULT hr = device.EnumChildItems(pEnum);
        if (hr.intValue() != WiaLib.S_OK) return null;

        WiaLib.IEnumWiaItem enumItems =
            new WiaLib.IEnumWiaItem(pEnum.getValue());
        try {
            PointerByReference pItem = new PointerByReference();
            hr = enumItems.Next(1, pItem, null);
            if (hr.intValue() != WiaLib.S_OK) return null;
            return new WiaLib.IWiaItem(pItem.getValue());
        } finally {
            enumItems.Release();
        }
    }

    private File transferToFile(WiaLib.IWiaItem item) throws ScanException {
        PointerByReference pXfer = new PointerByReference();
        HRESULT hr = item.queryDataTransfer(pXfer);
        if (hr.intValue() != WiaLib.S_OK) {
            throw new ScanException("Cannot get IWiaDataTransfer (hr=0x"
                                    + Integer.toHexString(hr.intValue()) + ")");
        }

        WiaLib.IWiaDataTransfer xfer =
            new WiaLib.IWiaDataTransfer(pXfer.getValue());
        try {
            File tempFile;
            try {
                tempFile = Files.createTempFile("scan4java_", ".bmp").toFile();
            } catch (IOException e) {
                throw new ScanException("Cannot create temp file", e);
            }

            WiaLib.STGMEDIUM medium = new WiaLib.STGMEDIUM();
            medium.tymed = WiaLib.TYMED_FILE;
            Memory pathMem = toWideString(tempFile.getAbsolutePath());
            medium.unionData = pathMem;
            medium.pUnkForRelease = null;
            medium.write();

            hr = xfer.idtGetData(medium, null);
            if (hr.intValue() != WiaLib.S_OK) {
                tempFile.delete();
                throw new ScanException("WIA transfer failed (hr=0x"
                                        + Integer.toHexString(hr.intValue()) + ")");
            }
            return tempFile;
        } finally {
            xfer.Release();
        }
    }

    private void writeIntProperty(WiaLib.IWiaPropertyStorage props,
                                int propId, int value) {
        WiaLib.PROPSPEC[]    specs = { new WiaLib.PROPSPEC(propId) };
        WiaLib.PROPVARIANT[] vars  = { new WiaLib.PROPVARIANT() };
        vars[0].vt   = WiaLib.PROPVARIANT.VT_I4;
        vars[0].data = Pointer.createConstant(value);
        vars[0].write();
        props.WriteMultiple(1, specs, vars, 2);
    }

    private static Memory toWideString(String s) {
        byte[] bytes = (s + "\0").getBytes(java.nio.charset.StandardCharsets.UTF_16LE);
        Memory m = new Memory(bytes.length);
        m.write(0, bytes, 0, bytes.length);
        return m;
    }

}