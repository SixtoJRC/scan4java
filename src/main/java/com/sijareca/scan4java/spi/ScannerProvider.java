package com.sijareca.scan4java.spi;

import com.sijareca.scan4java.Scanner;
import com.sijareca.scan4java.ScanException;

import java.util.List;

public interface ScannerProvider {

    /**
     * Returns true if this provider is available on the current system.
     * Called before getScanners() to avoid unnecessary initialization.
     */
    boolean isAvailable();

    /**
     * Returns all scanners detected by this provider.
     */
    List<Scanner> getScanners() throws ScanException;
}