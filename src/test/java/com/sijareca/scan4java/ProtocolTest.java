package com.sijareca.scan4java;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolTest {

    @Test
    void hasTwainAndWiaValues() {
        assertNotNull(Protocol.TWAIN);
        assertNotNull(Protocol.WIA);
        assertEquals(2, Protocol.values().length);
    }
}