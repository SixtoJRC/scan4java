package com.sijareca.scan4java.internal.wia;

import com.sijareca.scan4java.Scanner;
import com.sijareca.scan4java.ScanException;
import com.sijareca.scan4java.spi.ScannerProvider;

import java.util.ArrayList;
import java.util.List;

public class WiaScannerProvider implements ScannerProvider {

    @Override
    public boolean isAvailable() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win");
    }

    @Override
    public List<Scanner> getScanners() throws ScanException {
        if (!isAvailable()) return List.of();

        List<Scanner> result = new ArrayList<>();
        try (WiaSession session = new WiaSession()) {
            for (WiaDeviceInfo info : session.enumerateDevices()) {
                result.add(new WiaScanner(info.getId(), info.getName()));
            }
        } catch (ScanException e) {
            // WIA no disponible — devolvemos lista vacía
            return List.of();
        }
        return result;
    }
}