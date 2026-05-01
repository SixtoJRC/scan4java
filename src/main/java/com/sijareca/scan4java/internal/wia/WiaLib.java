package com.sijareca.scan4java.internal.wia;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
//import com.sun.jna.platform.win32.COM.COMUtils;
import com.sun.jna.platform.win32.COM.Unknown;
import com.sun.jna.platform.win32.Guid.GUID;
//import com.sun.jna.platform.win32.Guid.REFIID;
//import com.sun.jna.platform.win32.OaIdl.SAFEARRAY;
//import com.sun.jna.platform.win32.Ole32;
//import com.sun.jna.platform.win32.Variant.VARIANT;
//import com.sun.jna.platform.win32.WTypes.BSTR;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.ptr.PointerByReference;

class WiaLib {

    // CLSID del WIA Device Manager
    static final GUID CLSID_WiaDevMgr = new GUID(
        "{A1F4E726-8CF1-11D1-BF92-0060081ED811}");

    // IIDs de las interfaces WIA que usaremos
    static final GUID IID_IWiaDevMgr = new GUID(
        "{5eb2502a-8cf1-11d1-bf92-0060081ed811}");

    static final GUID IID_IEnumWIA_DEV_INFO = new GUID(
        "{5e38b83c-8cf1-11d1-bf92-0060081ed811}");

    static final GUID IID_IWiaPropertyStorage = new GUID(
        "{98B5E8A0-29CC-491a-AAC0-E6DB4FDCCEB6}");

    // Propiedades WIA que nos interesan
    static final int WIA_DIP_DEV_ID   = 2;   // Device ID (string)
    static final int WIA_DIP_DEV_NAME = 7;   // Device name (string)
    static final int WIA_DIP_DEV_TYPE = 5;   // Device type

    // Tipos de dispositivo WIA
    static final int StiDeviceTypeScanner = 0x0002;

    // HRESULT de éxito
    static final int S_OK    = 0x00000000;
    static final int S_FALSE = 0x00000001;

    /**
     * Interfaz IWiaDevMgr — gestiona los dispositivos WIA instalados.
     * Solo mapeamos los métodos que necesitamos.
     */
    static class IWiaDevMgr extends Unknown {

        IWiaDevMgr(Pointer p) {
            super(p);
        }

        // EnumDeviceInfo(lFlag, ppIEnum) — índice vtable 3 (tras QI, AddRef, Release)
        HRESULT EnumDeviceInfo(int lFlag, PointerByReference ppIEnum) {
            return (HRESULT) _invokeNativeObject(3,
                new Object[]{getPointer(), lFlag, ppIEnum},
                HRESULT.class);
        }
    }

    /**
     * Interfaz IEnumWIA_DEV_INFO — enumerador de dispositivos.
     */
    static class IEnumWiaDevInfo extends Unknown {

        IEnumWiaDevInfo(Pointer p) {
            super(p);
        }

        // Next(celt, rgelt, pceltFetched) — índice vtable 3
        HRESULT Next(int celt, PointerByReference rgelt, Pointer pceltFetched) {
            return (HRESULT) _invokeNativeObject(3,
                new Object[]{getPointer(), celt, rgelt, pceltFetched},
                HRESULT.class);
        }

        // Reset() — índice vtable 5
        HRESULT Reset() {
            return (HRESULT) _invokeNativeObject(5,
                new Object[]{getPointer()},
                HRESULT.class);
        }
    }

    /**
     * Interfaz IWiaPropertyStorage — lee propiedades de un dispositivo WIA.
     */
    static class IWiaPropertyStorage extends Unknown {

        IWiaPropertyStorage(Pointer p) {
            super(p);
        }

        // ReadMultiple(cpspec, rgpspec, rgpropvar) — índice vtable 3
        HRESULT ReadMultiple(int cpspec, PROPSPEC[] rgpspec, PROPVARIANT[] rgpropvar) {
            return (HRESULT) _invokeNativeObject(3,
                new Object[]{getPointer(), cpspec, rgpspec, rgpropvar},
                HRESULT.class);
        }
    }

    /**
     * PROPSPEC — especifica qué propiedad leer (por ID numérico).
     */
    @Structure.FieldOrder({"ulKind", "propid"})
    static class PROPSPEC extends Structure {
        public int ulKind = 1; // PRSPEC_PROPID = 1
        public int propid;

        PROPSPEC(int propid) {
            this.propid = propid;
            write();
        }
    }

    /**
     * PROPVARIANT simplificado — solo necesitamos leer strings (VT_BSTR)
     * y enteros (VT_I4) para nombre y tipo de dispositivo.
     */
    @Structure.FieldOrder({"vt", "reserved1", "reserved2", "reserved3", "data"})
    static class PROPVARIANT extends Structure {
        public short  vt;
        public short  reserved1;
        public short  reserved2;
        public short  reserved3;
        public Pointer data;   // para VT_BSTR apunta al BSTR; para VT_I4 es el valor

        public static final short VT_I4   = 3;
        public static final short VT_BSTR = 8;

        String getBSTR() {
            if (vt != VT_BSTR || data == null) return "";
            // BSTR: los 4 bytes antes del puntero son la longitud,
            // el contenido es UTF-16LE
            int len = data.getInt(-4);
            return new String(data.getByteArray(0, len),
                              java.nio.charset.StandardCharsets.UTF_16LE);
        }

        int getI4() {
            if (vt != VT_I4) return 0;
            return data == null ? 0 : (int) Pointer.nativeValue(data);
        }
    }
}