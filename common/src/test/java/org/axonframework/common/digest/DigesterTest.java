package org.axonframework.common.digest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DigesterTest {

    @Test
    void md5HexKnownValue() {
        assertEquals("5eb63bbbe01eeed093cb22bb8f5acdc3", Digester.md5Hex("hello world"));
    }

    @Test
    void updateAndDigestHex() {
        String first = Digester.newMD5Instance().update("hello".getBytes()).digestHex();
        String full = Digester.newMD5Instance().update("hello".getBytes()).update(" world".getBytes()).digestHex();

        assertEquals(32, full.length());
        assertTrue(first.length() == 32);
    }

    @Test
    void unknownAlgorithmThrowsAxonConfigurationException() {
        assertThrows(org.axonframework.common.AxonConfigurationException.class,
                () -> Digester.newInstance("NOPE"));
    }
}