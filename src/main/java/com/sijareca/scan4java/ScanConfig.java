package com.sijareca.scan4java;

public class ScanConfig {

    private int       dpi    = 200;
    private ColorMode color  = ColorMode.GRAYSCALE;
    private boolean   adf    = false;
    private boolean   duplex = false;

    public ScanConfig dpi(int dpi) {
        if (dpi <= 0) throw new IllegalArgumentException("dpi must be > 0, got " + dpi);
        this.dpi = dpi;
        return this;
    }

    public ScanConfig color(ColorMode color) {
        if (color == null) throw new NullPointerException("color must not be null");
        this.color = color;
        return this;
    }
    public ScanConfig adf(boolean adf)       { this.adf = adf;       return this; }
    public ScanConfig duplex(boolean duplex) { this.duplex = duplex; return this; }

    public int       getDpi()    { return dpi;    }
    public ColorMode getColor()  { return color;  }
    public boolean   isAdf()     { return adf;    }
    public boolean   isDuplex()  { return duplex; }

    public static ScanConfig defaults() {
        return new ScanConfig();
    }
}