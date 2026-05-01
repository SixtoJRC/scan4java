package com.sijareca.scan4java.internal.twain;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.win32.W32APIOptions;

interface TwainLib extends Library {

    // Constantes TWAIN
    int DG_CONTROL        = 0x0001;
    int DAT_IDENTITY      = 0x0003;
    int DAT_PARENT        = 0x0006;
    int DAT_IMAGENATIVEXFER = 0x0104;
    int DAT_PENDINGXFERS  = 0x0007;
    int DAT_USERINTERFACE = 0x0009;
    int DAT_IMAGEINFO     = 0x0101;
    int DG_IMAGE          = 0x0002;
    int DAT_CAPABILITY    = 0x0001;

    int MSG_OPENDSM       = 0x0301;
    int MSG_CLOSEDSM      = 0x0302;
    int MSG_OPENDS        = 0x0401;
    int MSG_CLOSEDS       = 0x0402;
    int MSG_ENABLEDS      = 0x0501;
    int MSG_DISABLEDS     = 0x0502;
    int MSG_GET           = 0x0001;
    int MSG_GETFIRST      = 0x0003;
    int MSG_GETNEXT       = 0x0004;
    int MSG_ENDXFER       = 0x0701;
    int MSG_SET           = 0x0006;
    int MSG_RESET         = 0x0007;

    int TWRC_SUCCESS      = 0x0000;
    int TWRC_FAILURE      = 0x0001;
    int TWRC_ENDOFLIST    = 0x0009;
    int TWRC_XFERDONE     = 0x0006;

    int DAT_EVENT         = 0x0002;
    int DAT_STATUS        = 0x0008;

    int MSG_GETDEFAULT    = 0x0003;
    int MSG_USERSELECT    = 0x0403;
    int MSG_PROCESSEVENT  = 0x0601;

    int TWRC_NOTDSEVENT   = 0x0005;
    int TWRC_CANCEL       = 0x0003;

    int MSG_XFERREADY     = 0x0101;
    int MSG_CLOSEDSREQ    = 0x0102;

    int TWCP_NONE         = 0x0000;

    // TW_VERSION: 2+2+2+2+34 = 42 bytes
    @Structure.FieldOrder({"MajorNum","MinorNum","Language","Country","Info"})
    class TW_VERSION extends Structure {
        public short  MajorNum;
        public short  MinorNum;
        public short  Language;
        public short  Country;
        public byte[] Info = new byte[34];
    }

    // TW_IDENTITY: 4 + TW_VERSION(42) + 4 + 34 + 34 + 34 = 156 bytes
    @Structure.FieldOrder({
        "Id","Version","ProtocolMajor","ProtocolMinor",
        "SupportedGroups","Manufacturer","ProductFamily","ProductName"
    })
    class TW_IDENTITY extends Structure {
        public int        Id;
        public TW_VERSION Version        = new TW_VERSION();
        public short      ProtocolMajor;
        public short      ProtocolMinor;
        public int        SupportedGroups;
        public byte[]     Manufacturer   = new byte[34];
        public byte[]     ProductFamily  = new byte[34];
        public byte[]     ProductName    = new byte[34];

        public String getManufacturer() { return Native.toString(Manufacturer); }
        public String getProductName()  { return Native.toString(ProductName);  }
    }

    @Structure.FieldOrder({"ShowUI","ModalUI","hParent"})
    class TW_USERINTERFACE extends Structure {
        public short   ShowUI;
        public short   ModalUI;
        public Pointer hParent;
    }

    @Structure.FieldOrder({"Count","EOJ"})
    class TW_PENDINGXFERS extends Structure {
        public short Count;
        public int   EOJ;
    }

    @Structure.FieldOrder({
        "XResolution","YResolution","ImageWidth","ImageLength",
        "SamplesPerPixel","BitsPerSample","BitsPerPixel",
        "Planar","PixelType","Compression"
    })
    class TW_IMAGEINFO extends Structure {
        public int   XResolution;
        public int   YResolution;
        public int   ImageWidth;
        public int   ImageLength;
        public short SamplesPerPixel;
        public short BitsPerSample;
        public short BitsPerPixel;
        public short Planar;
        public short PixelType;
        public short Compression;
    }

    @Structure.FieldOrder({"pEvent", "TWMessage"})
    class TW_EVENT extends Structure {
        public Pointer pEvent;
        public short   TWMessage;
    }

    @Structure.FieldOrder({"Cap", "ConType", "hContainer"})
    class TW_CAPABILITY extends Structure {
        public short   Cap;
        public short   ConType;
        public Pointer hContainer;
    }

    @Structure.FieldOrder({"ItemType", "Item"})
    class TW_ONEVALUE extends Structure {
        public short ItemType;
        public int   Item;
    }

    // Capability constants
    short ICAP_XRESOLUTION = 0x1118;
    short ICAP_YRESOLUTION = 0x1119;
    short ICAP_PIXELTYPE   = 0x0101;
    short ICAP_XFERMECH    = 0x0103;
    short TWSX_NATIVE      = 0x0000;
    short TWON_ONEVALUE    = 0x0005;
    short TWTY_UINT16      = 0x0004;
    short TWTY_FIX32       = 0x0007;

    @Structure.FieldOrder({"Whole", "Frac"})
    class TW_FIX32 extends Structure {
        public short Whole;
        public short Frac;
        public float toFloat() { return Whole + (float)(Frac & 0xFFFF) / 65536.0f; }
        public static TW_FIX32 fromFloat(float f) {
            TW_FIX32 fix = new TW_FIX32();
            fix.Whole = (short) f;
            fix.Frac  = (short)((f - fix.Whole) * 65536.0f);
            return fix;
        }
    }

    @Structure.FieldOrder({"ConditionCode", "Reserved"})
    class TW_STATUS extends Structure {
        public short ConditionCode;
        public short Reserved;
    }

    static TwainLib load() {
        return Native.load("TWAINDSM", TwainLib.class,
                           W32APIOptions.DEFAULT_OPTIONS);
    }

    int DSM_Entry(TW_IDENTITY origin, TW_IDENTITY dest,
                  int dg, int dat, int msg, Structure data);

    int DSM_Entry(TW_IDENTITY origin, TW_IDENTITY dest,
                  int dg, int dat, int msg, Pointer data);

    int DSM_Entry(TW_IDENTITY origin, TW_IDENTITY dest,
                  int dg, int dat, int msg, Memory data);

    int DSM_Entry(TW_IDENTITY origin, TW_IDENTITY dest,
                int dg, int dat, int msg, int[] data);

    int DSM_Entry(TW_IDENTITY origin, TW_IDENTITY dest,
                int dg, int dat, int msg, com.sun.jna.ptr.PointerByReference data);

}