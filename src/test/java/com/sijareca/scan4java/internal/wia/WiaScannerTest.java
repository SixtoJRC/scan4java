package com.sijareca.scan4java.internal.wia;

import com.sijareca.scan4java.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WiaScannerTest {

    private WiaScanner scanner;

    @BeforeEach
    void setUp() {
        scanner = new WiaScanner("{WIA-Canon-001}", "Canon MFP WIA");
    }

    @Test
    void getId() {
        assertEquals("{WIA-Canon-001}", scanner.getId());
    }

    @Test
    void getName() {
        assertEquals("Canon MFP WIA", scanner.getName());
    }

    @Test
    void getProtocolIsWia() {
        assertEquals(Protocol.WIA, scanner.getProtocol());
    }

    @Test
    void setDpiReturnsSelf() {
        assertSame(scanner, scanner.setDpi(200));
    }

    @Test
    void setColorReturnsSelf() {
        assertSame(scanner, scanner.setColor(ColorMode.BW));
    }

    @Test
    void setAdfReturnsSelf() {
        assertSame(scanner, scanner.setAdf(false));
    }

    @Test
    void setDuplexReturnsSelf() {
        assertSame(scanner, scanner.setDuplex(false));
    }

    @Test
    void scanWithoutHardwareThrowsScanException() {
        assertThrows(ScanException.class, () -> scanner.scan());
    }

    @Test
    void scanWithConfigWithoutHardwareThrowsScanException() {
        ScanConfig config = new ScanConfig().dpi(200).color(ColorMode.BW);
        assertThrows(ScanException.class, () -> scanner.scan(config));
    }
}