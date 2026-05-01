package com.sijareca.scan4java.internal.twain;

import com.sijareca.scan4java.Scanner;
import com.sijareca.scan4java.ScanException;
import com.sijareca.scan4java.spi.ScannerProvider;

import java.util.List;

public class TwainScannerProvider implements ScannerProvider {

    @Override
    public boolean isAvailable() {
        // TODO: intentar cargar TWAINDSM.DLL via JNA
        // Por ahora comprobamos que estamos en Windows 64-bit
        String os   = System.getProperty("os.name",    "").toLowerCase();
        String arch = System.getProperty("os.arch",    "").toLowerCase();
        return os.contains("win") && (arch.contains("64") || arch.contains("amd64"));
    }

    @Override
    public List<Scanner> getScanners() throws ScanException {
        if (!isAvailable()) return List.of();
        // TODO: enumerar fuentes TWAIN via DSM_Entry
        return List.of();
    }
}