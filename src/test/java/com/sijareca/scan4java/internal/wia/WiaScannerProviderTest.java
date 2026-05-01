package com.sijareca.scan4java.internal.wia;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.*;

class WiaScannerProviderTest {

    private final WiaScannerProvider provider = new WiaScannerProvider();

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void isAvailableOnWindowsReturnsTrue() {
        assertTrue(provider.isAvailable());
    }

    @Test
    void getScannersReturnsNonNullList() {
        assertDoesNotThrow(() -> {
            var result = provider.getScanners();
            assertNotNull(result);
        });
    }

    @Test
    void getScannersOnNonWindowsReturnsEmptyList() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            assertDoesNotThrow(() -> assertTrue(provider.getScanners().isEmpty()));
        }
    }
}