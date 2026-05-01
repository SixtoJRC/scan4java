package com.sijareca.scan4java;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ColorModeTest {

    @Test
    void hasThreeValues() {
        assertEquals(3, ColorMode.values().length);
    }

    @Test
    void containsExpectedModes() {
        assertNotNull(ColorMode.BW);
        assertNotNull(ColorMode.GRAYSCALE);
        assertNotNull(ColorMode.COLOR);
    }
}