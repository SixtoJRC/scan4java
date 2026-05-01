package com.sijareca.scan4java.internal.twain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.*;

class TwainScannerProviderTest {

    private final TwainScannerProvider provider = new TwainScannerProvider();

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void isAvailableOnWindows64ReturnsTrueOrFalse() {
        // En Windows 64-bit el resultado depende de si TWAINDSM.DLL está instalada
        // Lo importante es que no lanza excepción
        assertDoesNotThrow(() -> provider.isAvailable());
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
        // Si no es Windows, siempre lista vacía
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            assertDoesNotThrow(() -> {
                var result = provider.getScanners();
                assertTrue(result.isEmpty());
            });
        }
    }
}