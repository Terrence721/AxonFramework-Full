package org.axonframework.common.configuration;

import org.axonframework.common.TypeReference;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentNotFoundExceptionTest {

    @Test
    void messageIncludesTypeAndOptionalName() {
        ComponentNotFoundException named =
                new ComponentNotFoundException(String.class, "cmp");

        assertEquals("No component found for type [class java.lang.String] name [cmp].", named.getMessage());

        ComponentNotFoundException unnamed =
                new ComponentNotFoundException(String.class, null);

        assertEquals("No component found for type [class java.lang.String].", unnamed.getMessage());
    }

    @Test
    void supportsCauseAndTypeReference() {
        RuntimeException cause = new RuntimeException("root");
        ComponentNotFoundException exception =
                new ComponentNotFoundException(TypeReference.fromClass(Integer.class), "number", cause);

        assertTrue(exception.getCause() == cause);
        assertEquals("No component found for type [class java.lang.Integer] name [number].", exception.getMessage());
    }
}