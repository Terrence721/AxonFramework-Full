package org.axonframework.common.infra;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentDescriptorExceptionTest {

    @Test
    void constructsMessageAndCause() {
        RuntimeException cause = new RuntimeException("root");
        ComponentDescriptorException exception = new ComponentDescriptorException("bad descriptor", cause);

        assertTrue(exception.getMessage().equals("bad descriptor"));
        assertTrue(exception.getCause() == cause);
        assertTrue(exception instanceof org.axonframework.common.AxonNonTransientException);
    }
}