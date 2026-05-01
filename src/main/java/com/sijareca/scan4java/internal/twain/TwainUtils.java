package com.sijareca.scan4java.internal.twain;

class TwainUtils {

    static TwainLib.TW_IDENTITY buildAppIdentity() {
        TwainLib.TW_IDENTITY id = new TwainLib.TW_IDENTITY();
        id.Version.MajorNum  = 1;
        id.Version.MinorNum  = 0;
        id.ProtocolMajor     = 2;
        id.ProtocolMinor     = 3;
        id.SupportedGroups   = 3; // DG_CONTROL(1) | DG_IMAGE(2)
        copyString(id.Manufacturer,  "sijareca");
        copyString(id.ProductFamily, "scan4java");
        copyString(id.ProductName,   "scan4java");
        return id;
    }

    static void copyString(byte[] target, String value) {
        if (value == null) throw new NullPointerException("value must not be null");
        byte[] bytes = value.getBytes();
        System.arraycopy(bytes, 0, target, 0,
                         Math.min(bytes.length, target.length - 1));
    }
}
