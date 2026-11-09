package org.axonframework.common.configuration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentOverrideExceptionTest {

    @Test
    void messageIncludesTypeAndNameWhenProvided() {
        ComponentOverrideException exception = new ComponentOverrideException(String.class, "cmp");

        assertTrue(exception.getMessage().contains("cmp"));
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    void messageExplainsUnnamedOverride() {
        ComponentOverrideException exception = new ComponentOverrideException(Integer.class, null);

        assertTrue(exception.getMessage().contains("unique name is required"));
    }
}