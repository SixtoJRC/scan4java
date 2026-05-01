package com.sijareca.scan4java;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScanConfigTest {

    @Test
    void defaultValues() {
        ScanConfig config = ScanConfig.defaults();
        assertEquals(200,             config.getDpi());
        assertEquals(ColorMode.GRAYSCALE, config.getColor());
        assertFalse(config.isAdf());
        assertFalse(config.isDuplex());
    }

    @Test
    void fluentBuilderSetsAllValues() {
        ScanConfig config = ScanConfig.defaults()
            .dpi(300)
            .color(ColorMode.COLOR)
            .adf(true)
            .duplex(true);

        assertEquals(300,          config.getDpi());
        assertEquals(ColorMode.COLOR, config.getColor());
        assertTrue(config.isAdf());
        assertTrue(config.isDuplex());
    }

    @Test
    void isImmutable() {
        ScanConfig config = ScanConfig.defaults();
        assertNotSame(config, config.dpi(300));
        assertNotSame(config, config.color(ColorMode.BW));
        assertNotSame(config, config.adf(true));
        assertNotSame(config, config.duplex(true));
    }

    @Test
    void dpiNegativoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                     () -> ScanConfig.defaults().dpi(-1));
    }

    @Test
    void dpiCeroLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                     () -> ScanConfig.defaults().dpi(0));
    }

    @Test
    void colorNuloLanzaExcepcion() {
        assertThrows(NullPointerException.class,
                     () -> ScanConfig.defaults().color(null));
    }
}