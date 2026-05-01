package com.sijareca.scan4java;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScanExceptionTest {

    @Test
    void constructorWithMessage() {
        ScanException ex = new ScanException("test error");
        assertEquals("test error", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void constructorWithMessageAndCause() {
        RuntimeException cause = new RuntimeException("root cause");
        ScanException ex = new ScanException("wrapped", cause);
        assertEquals("wrapped", ex.getMessage());
        assertSame(cause, ex.getCause());
    }
}