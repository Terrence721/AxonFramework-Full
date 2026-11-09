package org.axonframework.common.configuration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AmbiguousComponentMatchExceptionTest {

    @Test
    void constructsRuntimeException() {
        AmbiguousComponentMatchException exception =
                new AmbiguousComponentMatchException(new Component.Identifier<>(String.class, "cmp"));

        assertTrue(exception instanceof RuntimeException);
        assertTrue(exception.getMessage().contains("cmp"));
    }
}