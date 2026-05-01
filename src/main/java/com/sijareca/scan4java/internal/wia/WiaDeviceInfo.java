package com.sijareca.scan4java.internal.wia;

class WiaDeviceInfo {

    private final String id;
    private final String name;
    private final int    type;

    WiaDeviceInfo(String id, String name, int type) {
        this.id   = id;
        this.name = name;
        this.type = type;
    }

    String getId()   { return id;   }
    String getName() { return name; }

    boolean isScanner() {
        return (type & WiaLib.StiDeviceTypeScanner) != 0;
    }
}