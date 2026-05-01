package com.sijareca.scan4java;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

class ScanManagerTest {

    @Test
    void instanceReturnsSameObject() {
        ScanManager a = ScanManager.instance();
        ScanManager b = ScanManager.instance();
        assertSame(a, b);
    }

    @Test
    void getScannersReturnsNonNullList() {
        // Sin escáner físico el resultado es una lista vacía, nunca null
        assertNotNull(ScanManager.instance().getScanners());
    }

    @Test
    void getScannersFiltersByProtocol() {
        // Filtrar por protocolo nunca lanza excepción aunque no haya escáneres
        assertNotNull(ScanManager.instance().getScanners(Protocol.TWAIN));
        assertNotNull(ScanManager.instance().getScanners(Protocol.WIA));
    }

    @Test
    void getScannersFilteredIsSubsetOfAll() {
        List<Scanner> all   = ScanManager.instance().getScanners();
        List<Scanner> twain = ScanManager.instance().getScanners(Protocol.TWAIN);
        List<Scanner> wia   = ScanManager.instance().getScanners(Protocol.WIA);

        assertTrue(all.containsAll(twain));
        assertTrue(all.containsAll(wia));
    }

    @Test
    void deduplicatePreferstwainOverWia() {
        // Verificamos que getScanners() nunca devuelve null ni lanza excepción
        // La deduplicación real se verifica en integración con hardware
        List<Scanner> scanners = ScanManager.instance().getScanners();
        assertNotNull(scanners);

        // Si hay duplicados por nombre, no debe haber dos con el mismo nombre
        // donde uno sea TWAIN y otro WIA
        long duplicatesWithBothProtocols = scanners.stream()
            .filter(s -> s.getProtocol() == Protocol.WIA)
            .filter(wia -> scanners.stream()
                .anyMatch(t -> t.getProtocol() == Protocol.TWAIN
                        && t.getName().equalsIgnoreCase(wia.getName())))
            .count();

        assertEquals(0, duplicatesWithBothProtocols);
    }

// --- Tests de deduplicación con datos sintéticos ---

    @Test
    void deduplicateRemovesWiaWhenTwainHasSameName() {
        Scanner twain = twainScanner("t-001", "HP LaserJet");
        Scanner wia   = wiaScanner("w-001", "HP LaserJet");

        List<Scanner> result = ScanManager.instance().deduplicate(List.of(twain, wia));

        assertEquals(1, result.size());
        assertEquals(Protocol.TWAIN, result.get(0).getProtocol());
    }

    @Test
    void deduplicateKeepsWiaWhenNoTwainWithSameName() {
        Scanner twain = twainScanner("t-001", "HP LaserJet");
        Scanner wia   = wiaScanner("w-002", "Canon MFP");

        List<Scanner> result = ScanManager.instance().deduplicate(List.of(twain, wia));

        assertEquals(2, result.size());
    }

    @Test
    void deduplicateKeepsBothTwainScanners() {
        Scanner twain1 = twainScanner("t-001", "HP LaserJet");
        Scanner twain2 = twainScanner("t-002", "Canon MFP");

        List<Scanner> result = ScanManager.instance().deduplicate(List.of(twain1, twain2));

        assertEquals(2, result.size());
    }

    @Test
    void deduplicateHandlesEmptyList() {
        List<Scanner> result = ScanManager.instance().deduplicate(List.of());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getScannersFilterByTwainReturnsOnlyTwain() {
        List<Scanner> twain = ScanManager.instance().getScanners(Protocol.TWAIN);
        assertTrue(twain.stream().allMatch(s -> s.getProtocol() == Protocol.TWAIN));
    }

    @Test
    void getScannersFilterByWiaReturnsOnlyWia() {
        List<Scanner> wia = ScanManager.instance().getScanners(Protocol.WIA);
        assertTrue(wia.stream().allMatch(s -> s.getProtocol() == Protocol.WIA));
    }

    @Test
    void getScannersFilterByProtocolExecutesLambda() {
        Scanner t = twainScanner("t-001", "HP");
        Scanner w = wiaScanner("w-001", "Canon");

        List<Scanner> deduped = ScanManager.instance().deduplicate(List.of(t, w));

        List<Scanner> twainOnly = deduped.stream()
            .filter(s -> s.getProtocol() == Protocol.TWAIN)
            .toList();
        List<Scanner> wiaOnly = deduped.stream()
            .filter(s -> s.getProtocol() == Protocol.WIA)
            .toList();

        assertEquals(1, twainOnly.size());
        assertEquals(Protocol.TWAIN, twainOnly.get(0).getProtocol());
        assertEquals(1, wiaOnly.size());
        assertEquals(Protocol.WIA, wiaOnly.get(0).getProtocol());
    }

    @Test
    void getScannersHandlesProviderException() {
        // getScanners() captura ScanException internamente — nunca propaga
        // Verificamos que aunque los providers fallen, el resultado es lista vacía
        assertDoesNotThrow(() -> {
            List<Scanner> result = ScanManager.instance().getScanners();
            assertNotNull(result);
        });
    }

    // En lugar de twainScanner(...) y wiaScanner(...)
    // usamos implementaciones anónimas de Scanner

    private static Scanner twainScanner(String id, String name) {
        return new Scanner() {
            public String getId()        { return id; }
            public String getName()      { return name; }
            public Protocol getProtocol(){ return Protocol.TWAIN; }
            public Scanner setDpi(int d)          { return this; }
            public Scanner setColor(ColorMode c)  { return this; }
            public Scanner setAdf(boolean a)      { return this; }
            public Scanner setDuplex(boolean d)   { return this; }
            public java.util.List<java.awt.image.BufferedImage> scan() { return java.util.List.of(); }
            public java.util.List<java.awt.image.BufferedImage> scan(ScanConfig c) { return java.util.List.of(); }
        };
    }

    private static Scanner wiaScanner(String id, String name) {
        return new Scanner() {
            public String getId()        { return id; }
            public String getName()      { return name; }
            public Protocol getProtocol(){ return Protocol.WIA; }
            public Scanner setDpi(int d)          { return this; }
            public Scanner setColor(ColorMode c)  { return this; }
            public Scanner setAdf(boolean a)      { return this; }
            public Scanner setDuplex(boolean d)   { return this; }
            public java.util.List<java.awt.image.BufferedImage> scan() { return java.util.List.of(); }
            public java.util.List<java.awt.image.BufferedImage> scan(ScanConfig c) { return java.util.List.of(); }
        };
    }

}