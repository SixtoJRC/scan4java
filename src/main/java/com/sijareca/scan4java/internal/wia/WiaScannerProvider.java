package com.sijareca.scan4java.internal.wia;

import com.sijareca.scan4java.Scanner;
import com.sijareca.scan4java.ScanException;
import com.sijareca.scan4java.spi.ScannerProvider;

import java.util.List;

public class WiaScannerProvider implements ScannerProvider {

    @Override
    public boolean isAvailable() {
        // WIA es nativo en Windows — misma comprobación de OS
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win");
    }

    @Override
    public List<Scanner> getScanners() throws ScanException {
        if (!isAvailable()) return List.of();
        // TODO: enumerar dispositivos WIA via COM/JNA
        return List.of();
    }
}