package com.sijareca.scan4java.internal.twain;

import com.sijareca.scan4java.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TwainScannerTest {

    private TwainScanner scanner;

    @BeforeEach
    void setUp() {
        scanner = new TwainScanner("twain-001", "HP LaserJet TWAIN");
    }

    @Test
    void getId() {
        assertEquals("twain-001", scanner.getId());
    }

    @Test
    void getName() {
        assertEquals("HP LaserJet TWAIN", scanner.getName());
    }

    @Test
    void getProtocolIsTwain() {
        assertEquals(Protocol.TWAIN, scanner.getProtocol());
    }

    @Test
    void setDpiReturnsSelf() {
        assertSame(scanner, scanner.setDpi(300));
    }

    @Test
    void setColorReturnsSelf() {
        assertSame(scanner, scanner.setColor(ColorMode.COLOR));
    }

    @Test
    void setAdfReturnsSelf() {
        assertSame(scanner, scanner.setAdf(true));
    }

    @Test
    void setDuplexReturnsSelf() {
        assertSame(scanner, scanner.setDuplex(true));
    }

    @Test
    void scanWithoutHardwareThrowsScanException() {
        // Sin TWAINDSM.DLL o escáner, debe lanzar ScanException — nunca NPE ni error inesperado
        assertThrows(ScanException.class, () -> scanner.scan());
    }

    @Test
    void scanWithConfigWithoutHardwareThrowsScanException() {
        ScanConfig config = ScanConfig.defaults().dpi(300).color(ColorMode.GRAYSCALE);
        assertThrows(ScanException.class, () -> scanner.scan(config));
    }
}