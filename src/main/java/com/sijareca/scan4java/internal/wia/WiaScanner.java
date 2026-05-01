package com.sijareca.scan4java.internal.wia;

import com.sijareca.scan4java.*;

import java.awt.image.BufferedImage;
import java.util.List;

public class WiaScanner implements Scanner {

    private final String id;
    private final String name;
    private ScanConfig config = ScanConfig.defaults();

    WiaScanner(String id, String name) {
        this.id   = id;
        this.name = name;
    }

    @Override public String   getId()       { return id;          }
    @Override public String   getName()     { return name;        }
    @Override public Protocol getProtocol() { return Protocol.WIA; }

    @Override
    public Scanner setDpi(int dpi) {
        config.dpi(dpi);
        return this;
    }

    @Override
    public Scanner setColor(ColorMode mode) {
        config.color(mode);
        return this;
    }

    @Override
    public Scanner setAdf(boolean adf) {
        config.adf(adf);
        return this;
    }

    @Override
    public Scanner setDuplex(boolean duplex) {
        config.duplex(duplex);
        return this;
    }

    @Override
    public List<BufferedImage> scan() throws ScanException {
        return scan(config);
    }

    @Override
    public List<BufferedImage> scan(ScanConfig config) throws ScanException {
        // TODO: implementación JNA WIA COM
        throw new ScanException("WIA scan not yet implemented");
    }
}