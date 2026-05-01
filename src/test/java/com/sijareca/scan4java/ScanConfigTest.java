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
        ScanConfig config = new ScanConfig()
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
    void fluentBuilderReturnsItself() {
        ScanConfig config = new ScanConfig();
        assertSame(config, config.dpi(300));
        assertSame(config, config.color(ColorMode.BW));
        assertSame(config, config.adf(true));
        assertSame(config, config.duplex(true));
    }
}