package org.axonframework.common.io;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IOUtilsTest {

    @Test
    void closeQuietlySuppressesExceptionAndAcceptsNull() {
        IOUtils.closeQuietly(null);

        AutoCloseable closeable = () -> {
            throw new RuntimeException("boom");
        };

        IOUtils.closeQuietly(closeable);
    }

    @Test
    void closeQuietlyIfCloseableHandlesCloseableOnly() {
        boolean[] closed = new boolean[1];

        IOUtils.closeQuietlyIfCloseable((AutoCloseable) () -> closed[0] = true);
        assertTrue(closed[0]);

        IOUtils.closeQuietlyIfCloseable("not-closeable");
        assertEquals(java.nio.charset.StandardCharsets.UTF_8, IOUtils.UTF8);
    }
}