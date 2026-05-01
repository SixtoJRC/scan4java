package com.sijareca.scan4java;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

class ScanManagerTest {

    @Test
    void instanceReturnsSameObject() {
        ScanManager a = ScanManager.instance();
        ScanManager b = ScanManager.instance();
        assertSame(a, b);
    }

    @Test
    void getScannersReturnsNonNullList() {
        // Sin escáner físico el resultado es una lista vacía, nunca null
        assertNotNull(ScanManager.instance().getScanners());
    }

    @Test
    void getScannersFiltersByProtocol() {
        // Filtrar por protocolo nunca lanza excepción aunque no haya escáneres
        assertNotNull(ScanManager.instance().getScanners(Protocol.TWAIN));
        assertNotNull(ScanManager.instance().getScanners(Protocol.WIA));
    }

    @Test
    void getScannersFilteredIsSubsetOfAll() {
        List<Scanner> all   = ScanManager.instance().getScanners();
        List<Scanner> twain = ScanManager.instance().getScanners(Protocol.TWAIN);
        List<Scanner> wia   = ScanManager.instance().getScanners(Protocol.WIA);

        assertTrue(all.containsAll(twain));
        assertTrue(all.containsAll(wia));
    }
}