package com.sijareca.scan4java.internal.wia;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.COM.Unknown;
import com.sun.jna.platform.win32.Guid.GUID;
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

    // IID de IWiaDevMgr — GetImageDlg
    static final GUID IID_IWiaItem = new GUID(
        "{4db1ad10-3391-11d2-9a33-00c04fa36145}");

    // Propiedades de resolución y color del dispositivo
    static final int WIA_DPS_HORIZONTAL_RESOLUTION = 6147;
    static final int WIA_DPS_VERTICAL_RESOLUTION   = 6148;
    static final int WIA_IPA_DATATYPE              = 4103;
    static final int WIA_DPS_DOCUMENT_HANDLING_SELECT = 3088;
    static final int FEEDER  = 0x001;
    static final int FLATBED = 0x002;
    static final int DUPLEX  = 0x004;

    // Propiedades de formato de salida
    static final int WIA_IPA_FORMAT       = 4106;
    static final int WIA_IPA_TYMED        = 4108;
    static final int TYMED_FILE           = 2;

    // GUID formato BMP
    static final GUID WiaImgFmt_BMP = new GUID(
        "{B96B3CAB-0728-11D3-9D7B-0000F81EF32E}");

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
        
        // CreateDevice(bstrDeviceID, ppWiaItemRoot) — índice vtable 6
        HRESULT CreateDevice(com.sun.jna.platform.win32.WTypes.BSTR bstrDeviceID,
                            PointerByReference ppWiaItemRoot) {
            return (HRESULT) _invokeNativeObject(6,
                new Object[]{getPointer(), bstrDeviceID, ppWiaItemRoot},
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

        HRESULT WriteMultiple(int cpspec, PROPSPEC[] rgpspec,
                            PROPVARIANT[] rgpropvar, int propidNameFirst) {
            return (HRESULT) _invokeNativeObject(4,
                new Object[]{getPointer(), cpspec, rgpspec, rgpropvar, propidNameFirst},
                HRESULT.class);
        }
    }

    /**
     * IWiaItem — representa un dispositivo o item WIA.
     * Solo mapeamos EnumChildItems y GetItemType.
     */
    static class IWiaItem extends Unknown {

        IWiaItem(Pointer p) {
            super(p);
        }

        // GetItemType() — índice vtable 3
        HRESULT GetItemType(Pointer pItemType) {
            return (HRESULT) _invokeNativeObject(3,
                new Object[]{getPointer(), pItemType},
                HRESULT.class);
        }

        // EnumChildItems(ppIEnumWiaItem) — índice vtable 8
        HRESULT EnumChildItems(PointerByReference ppIEnumWiaItem) {
            return (HRESULT) _invokeNativeObject(8,
                new Object[]{getPointer(), ppIEnumWiaItem},
                HRESULT.class);
        }

        // QueryInterface para obtener IWiaPropertyStorage
        HRESULT queryPropertyStorage(PointerByReference ppProps) {
            return (HRESULT) _invokeNativeObject(0,
                new Object[]{getPointer(), IID_IWiaPropertyStorage, ppProps},
                HRESULT.class);
        }

        // QueryInterface para obtener IWiaDataTransfer
        HRESULT queryDataTransfer(PointerByReference ppXfer) {
            return (HRESULT) _invokeNativeObject(0,
                new Object[]{getPointer(), IWiaDataTransfer.IID, ppXfer},
                HRESULT.class);
        }
    }

    /**
     * IEnumWiaItem — enumerador de items hijo de un dispositivo.
     */
    static class IEnumWiaItem extends Unknown {

        IEnumWiaItem(Pointer p) {
            super(p);
        }

        // Next(celt, rgelt, pceltFetched) — índice vtable 3
        HRESULT Next(int celt, PointerByReference rgelt, Pointer pceltFetched) {
            return (HRESULT) _invokeNativeObject(3,
                new Object[]{getPointer(), celt, rgelt, pceltFetched},
                HRESULT.class);
        }
    }

    /**
     * IWiaDataTransfer — transfiere datos desde un item WIA a fichero.
     */
    static class IWiaDataTransfer extends Unknown {

        // IID de IWiaDataTransfer
        static final GUID IID = new GUID(
            "{a6cef998-a5b0-11d2-a08f-00c04f72dc3c}");

        IWiaDataTransfer(Pointer p) {
            super(p);
        }

        // idtGetData(pMedium, pCallback) — índice vtable 3
        // pCallback = null → sin notificaciones de progreso
        HRESULT idtGetData(STGMEDIUM pMedium, Pointer pCallback) {
            return (HRESULT) _invokeNativeObject(3,
                new Object[]{getPointer(), pMedium, pCallback},
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

        public void clear() {
            WiaLib.PropVariantClear(getPointer());
        }
    }

    static void PropVariantClear(Pointer pvar) {
        com.sun.jna.Function.getFunction("ole32", "PropVariantClear")
            .invokeInt(new Object[]{pvar});
    }

    // STGMEDIUM simplificado — solo necesitamos TYMED_FILE
    @Structure.FieldOrder({"tymed", "unionData", "pUnkForRelease"})
    static class STGMEDIUM extends Structure {
        public int     tymed;         // TYMED_FILE = 2
        public Pointer unionData;     // lpszFileName como LPOLESTR
        public Pointer pUnkForRelease;
    }



}