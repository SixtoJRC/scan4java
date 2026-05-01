package com.sijareca.scan4java;

public class ScanConfig {

    private final int       dpi;
    private final ColorMode color;
    private final boolean   adf;
    private final boolean   duplex;

    private ScanConfig(int dpi, ColorMode color, boolean adf, boolean duplex) {
        this.dpi    = dpi;
        this.color  = color;
        this.adf    = adf;
        this.duplex = duplex;
    }

    public static ScanConfig defaults() {
        return new ScanConfig(200, ColorMode.GRAYSCALE, false, false);
    }

    public ScanConfig dpi(int dpi) {
        if (dpi <= 0) throw new IllegalArgumentException("dpi must be > 0, got " + dpi);
        return new ScanConfig(dpi, this.color, this.adf, this.duplex);
    }

    public ScanConfig color(ColorMode color) {
        if (color == null) throw new NullPointerException("color must not be null");
        return new ScanConfig(this.dpi, color, this.adf, this.duplex);
    }

    public ScanConfig adf(boolean adf) {
        return new ScanConfig(this.dpi, this.color, adf, this.duplex);
    }

    public ScanConfig duplex(boolean duplex) {
        return new ScanConfig(this.dpi, this.color, this.adf, duplex);
    }

    public int       getDpi()    { return dpi;    }
    public ColorMode getColor()  { return color;  }
    public boolean   isAdf()     { return adf;    }
    public boolean   isDuplex()  { return duplex; }
}