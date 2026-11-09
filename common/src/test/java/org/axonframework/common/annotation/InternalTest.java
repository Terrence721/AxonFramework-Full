package org.axonframework.common.annotation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InternalTest {

    @Internal
    static class InternalComponent {
    }

    @Test
    void internalAnnotationIsPresent() {
        assertTrue(InternalComponent.class.isAnnotationPresent(Internal.class));
        assertNotNull(InternalComponent.class.getAnnotation(Internal.class));
    }
}