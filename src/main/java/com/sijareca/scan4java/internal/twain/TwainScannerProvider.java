package com.sijareca.scan4java.internal.twain;

import com.sijareca.scan4java.Scanner;
import com.sijareca.scan4java.ScanException;
import com.sijareca.scan4java.spi.ScannerProvider;

import java.util.ArrayList;
import java.util.List;

public class TwainScannerProvider implements ScannerProvider {

    @Override
    public boolean isAvailable() {
        String os   = System.getProperty("os.name", "").toLowerCase();
        String arch = System.getProperty("os.arch", "").toLowerCase();
        if (!os.contains("win") || (!arch.contains("64") && !arch.contains("amd64"))) {
            return false;
        }
        // Verificamos que TWAINDSM.DLL esté accesible
        try {
            TwainLib.load();
            return true;
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    @Override
    public List<Scanner> getScanners() throws ScanException {
        if (!isAvailable()) return List.of();

        List<Scanner> result = new ArrayList<>();
        try (TwainSession session = new TwainSession(TwainLib.load())) {
            for (TwainLib.TW_IDENTITY ds : session.enumerateSources()) {
                result.add(new TwainScanner(
                    String.valueOf(ds.Id),
                    ds.getProductName()
                ));
            }
        }
        return result;
    }
}