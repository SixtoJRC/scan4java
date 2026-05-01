package com.sijareca.scan4java.internal.wia;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WiaDeviceInfoTest {

    @Test
    void getIdAndName() {
        WiaDeviceInfo info = new WiaDeviceInfo("{WIA-001}", "Canon MFP", 0x0002);
        assertEquals("{WIA-001}", info.getId());
        assertEquals("Canon MFP",  info.getName());
    }

    @Test
    void isScannerWhenTypeContainsScannerBit() {
        WiaDeviceInfo scanner = new WiaDeviceInfo("id", "name", 0x0002);
        assertTrue(scanner.isScanner());
    }

    @Test
    void isNotScannerWhenTypeIsCameraOnly() {
        // StiDeviceTypeDigitalCamera = 0x0001
        WiaDeviceInfo camera = new WiaDeviceInfo("id", "name", 0x0001);
        assertFalse(camera.isScanner());
    }

    @Test
    void isScannerWhenTypeHasScannerBitCombined() {
        // Tipo combinado que incluye el bit de escáner
        WiaDeviceInfo combined = new WiaDeviceInfo("id", "name", 0x0003);
        assertTrue(combined.isScanner());
    }
}