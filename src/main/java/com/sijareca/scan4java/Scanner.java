package com.sijareca.scan4java;

import java.awt.image.BufferedImage;
import java.util.List;

public interface Scanner {

    String   getId();
    String   getName();
    Protocol getProtocol();

    Scanner setDpi(int dpi);
    Scanner setColor(ColorMode mode);
    Scanner setAdf(boolean adf);
    Scanner setDuplex(boolean duplex);

    List<BufferedImage> scan()              throws ScanException;
    List<BufferedImage> scan(ScanConfig config) throws ScanException;
}