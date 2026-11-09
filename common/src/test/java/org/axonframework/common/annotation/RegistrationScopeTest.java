package org.axonframework.common.annotation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RegistrationScopeTest {

    @RegistrationScope
    static class CurrentScoped {
    }

    @RegistrationScope(scope = RegistrationScope.Scope.CHILDREN, value = "reason")
    static class ChildScoped {
    }

    @Test
    void defaults() {
        RegistrationScope annotation = CurrentScoped.class.getAnnotation(RegistrationScope.class);
        assertNotNull(annotation);
        assertEquals("", annotation.value());
        assertEquals(RegistrationScope.Scope.CURRENT, annotation.scope());
    }

    @Test
    void explicitScope() {
        RegistrationScope annotation = ChildScoped.class.getAnnotation(RegistrationScope.class);
        assertNotNull(annotation);
        assertEquals("reason", annotation.value());
        assertEquals(RegistrationScope.Scope.CHILDREN, annotation.scope());
    }

    @Test
    void scopeEnumValues() {
        assertEquals(2, RegistrationScope.Scope.values().length);
        assertEquals(RegistrationScope.Scope.CURRENT, RegistrationScope.Scope.valueOf("CURRENT"));
        assertEquals(RegistrationScope.Scope.CHILDREN, RegistrationScope.Scope.valueOf("CHILDREN"));
    }
}