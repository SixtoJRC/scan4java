package com.sijareca.scan4java;

import com.sijareca.scan4java.internal.twain.TwainScannerProvider;
import com.sijareca.scan4java.internal.wia.WiaScannerProvider;
import com.sijareca.scan4java.spi.ScannerProvider;

import java.util.ArrayList;
import java.util.List;

public class ScanManager {

    private static ScanManager INSTANCE;

    private final List<ScannerProvider> providers = List.of(
        new TwainScannerProvider(),
        new WiaScannerProvider()
    );

    private ScanManager() {}

    public static ScanManager instance() {
        if (INSTANCE == null) INSTANCE = new ScanManager();
        return INSTANCE;
    }

    public List<Scanner> getScanners() {
        List<Scanner> result = new ArrayList<>();
        for (ScannerProvider provider : providers) {
            if (!provider.isAvailable()) continue;
            try {
                result.addAll(provider.getScanners());
            } catch (ScanException e) {
                // provider falló → se ignora, no rompe el conjunto
            }
        }
        return deduplicate(result);
    }

    public List<Scanner> getScanners(Protocol protocol) {
        return getScanners().stream()
            .filter(s -> s.getProtocol() == protocol)
            .toList();
    }

    // Si un dispositivo aparece en TWAIN y WIA, TWAIN tiene preferencia
    private List<Scanner> deduplicate(List<Scanner> scanners) {
        List<Scanner> result = new ArrayList<>();
        for (Scanner candidate : scanners) {
            boolean shadowed = result.stream().anyMatch(existing ->
                existing.getName().equalsIgnoreCase(candidate.getName()) &&
                existing.getProtocol() == Protocol.TWAIN
            );
            if (!shadowed) result.add(candidate);
        }
        return result;
    }
}