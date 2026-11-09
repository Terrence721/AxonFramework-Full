package org.axonframework.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessRetriesExhaustedExceptionTest {

    @Test
    void constructsMessageAndCause() {
        RuntimeException cause = new RuntimeException("root");
        ProcessRetriesExhaustedException exception = new ProcessRetriesExhaustedException("retries exhausted", cause);

        assertEquals("retries exhausted", exception.getMessage());
        assertTrue(exception.getCause() == cause);
    }

    @Test
    void isAxonException() {
        assertTrue(new ProcessRetriesExhaustedException("retries exhausted") instanceof AxonException);
    }
}