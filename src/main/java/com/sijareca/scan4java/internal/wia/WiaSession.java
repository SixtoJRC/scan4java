package com.sijareca.scan4java.internal.wia;

import com.sijareca.scan4java.ScanException;
import com.sun.jna.platform.win32.Ole32;
import com.sun.jna.platform.win32.OleAuto;
import com.sun.jna.platform.win32.WTypes;
import com.sun.jna.ptr.PointerByReference;

import java.util.ArrayList;
import java.util.List;

class WiaSession implements AutoCloseable {

    private boolean comInitialized = false;

    WiaSession() throws ScanException {
        // Inicializamos COM en este hilo
        var hr = Ole32.INSTANCE.CoInitializeEx(null,
                     Ole32.COINIT_APARTMENTTHREADED);
        // S_OK o S_FALSE (ya inicializado) son ambos aceptables
        if (hr.intValue() != WiaLib.S_OK &&
            hr.intValue() != WiaLib.S_FALSE) {
            throw new ScanException("Cannot initialize COM (hr=0x"
                                    + Integer.toHexString(hr.intValue()) + ")");
        }
        comInitialized = true;
    }

    List<WiaDeviceInfo> enumerateDevices() throws ScanException {
        List<WiaDeviceInfo> result = new ArrayList<>();

        // Crear instancia del WIA Device Manager via CoCreateInstance
        PointerByReference pDevMgr = new PointerByReference();
        var hr = Ole32.INSTANCE.CoCreateInstance(
            WiaLib.CLSID_WiaDevMgr,
            null,
            WTypes.CLSCTX_LOCAL_SERVER,
            WiaLib.IID_IWiaDevMgr,
            pDevMgr);

        if (hr.intValue() != WiaLib.S_OK) {
            // WIA no disponible en este sistema — devolvemos lista vacía
            return result;
        }

        WiaLib.IWiaDevMgr devMgr = new WiaLib.IWiaDevMgr(pDevMgr.getValue());

        try {
            PointerByReference pEnum = new PointerByReference();
            hr = devMgr.EnumDeviceInfo(0, pEnum);
            if (hr.intValue() != WiaLib.S_OK) return result;

            WiaLib.IEnumWiaDevInfo enumDevInfo =
                new WiaLib.IEnumWiaDevInfo(pEnum.getValue());

            try {
                enumDevInfo.Reset();

                PointerByReference pPropStorage = new PointerByReference();
                while (enumDevInfo.Next(1, pPropStorage, null)
                                  .intValue() == WiaLib.S_OK) {

                    WiaLib.IWiaPropertyStorage propStorage =
                        new WiaLib.IWiaPropertyStorage(pPropStorage.getValue());

                    WiaDeviceInfo info = readDeviceInfo(propStorage);
                    propStorage.Release();

                    if (info != null && info.isScanner()) {
                        result.add(info);
                    }
                }
            } finally {
                enumDevInfo.Release();
            }
        } finally {
            devMgr.Release();
        }

        return result;
    }

    private WiaDeviceInfo readDeviceInfo(WiaLib.IWiaPropertyStorage props) {
        WiaLib.PROPSPEC[] specs = {
            new WiaLib.PROPSPEC(WiaLib.WIA_DIP_DEV_ID),
            new WiaLib.PROPSPEC(WiaLib.WIA_DIP_DEV_NAME),
            new WiaLib.PROPSPEC(WiaLib.WIA_DIP_DEV_TYPE)
        };
        WiaLib.PROPVARIANT[] variants = {
            new WiaLib.PROPVARIANT(),
            new WiaLib.PROPVARIANT(),
            new WiaLib.PROPVARIANT()
        };

        var hr = props.ReadMultiple(3, specs, variants);
        if (hr.intValue() != WiaLib.S_OK) return null;

        String id   = variants[0].getBSTR();
        String name = variants[1].getBSTR();
        int    type = variants[2].getI4();

        return new WiaDeviceInfo(id, name, type);
    }

    @Override
    public void close() {
        if (comInitialized) {
            Ole32.INSTANCE.CoUninitialize();
            comInitialized = false;
        }
    }

    /**
     * Abre un dispositivo WIA por ID y devuelve su IWiaItem raíz.
     * El llamador es responsable de llamar Release() cuando termine.
     */
    WiaLib.IWiaItem openDevice(String deviceId) throws ScanException {
        PointerByReference pDevMgr = new PointerByReference();
        var hr = Ole32.INSTANCE.CoCreateInstance(
            WiaLib.CLSID_WiaDevMgr, null,
            WTypes.CLSCTX_LOCAL_SERVER,
            WiaLib.IID_IWiaDevMgr,
            pDevMgr);

        if (hr.intValue() != WiaLib.S_OK) {
            throw new ScanException("Cannot create WIA DevMgr (hr=0x"
                                    + Integer.toHexString(hr.intValue()) + ")");
        }

        WiaLib.IWiaDevMgr devMgr = new WiaLib.IWiaDevMgr(pDevMgr.getValue());
        try {
            // Crear BSTR con SysAllocString — forma correcta y no deprecada
            com.sun.jna.platform.win32.WTypes.BSTR bstrId =
                OleAuto.INSTANCE.SysAllocString(deviceId);
            try {
                PointerByReference pDevice = new PointerByReference();
                hr = devMgr.CreateDevice(bstrId, pDevice);

                if (hr.intValue() != WiaLib.S_OK) {
                    throw new ScanException("Cannot open WIA device '" + deviceId
                                            + "' (hr=0x"
                                            + Integer.toHexString(hr.intValue()) + ")");
                }
                return new WiaLib.IWiaItem(pDevice.getValue());
            } finally {
                // Liberar el BSTR siempre
                OleAuto.INSTANCE.SysFreeString(bstrId);
            }
        } finally {
            devMgr.Release();
        }
    }

}