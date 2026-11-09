package org.axonframework.common.configuration;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentIdentifierTest {

    @Test
    void validatesIdentifierInputs() {
        assertThrows(NullPointerException.class,
            () -> new org.axonframework.common.configuration.Component.Identifier<String>((Class<String>) null, "name"));
        assertThrows(IllegalArgumentException.class,
                () -> new org.axonframework.common.configuration.Component.Identifier<>(String.class, ""));
    }

    @Test
    void matchesByRawTypeAndName() {
        org.axonframework.common.configuration.Component.Identifier<Object> first = new org.axonframework.common.configuration.Component.Identifier<>(Object.class, "cmp");
        org.axonframework.common.configuration.Component.Identifier<String> second = new org.axonframework.common.configuration.Component.Identifier<>(String.class, "cmp");

        assertTrue(first.matches(second));
        assertFalse(first.matches(new org.axonframework.common.configuration.Component.Identifier<>(String.class, "other")));
    }

    @Test
    void matchesByTypeReference() {
        org.axonframework.common.TypeReference<java.util.List<String>> typeRef =
                new org.axonframework.common.TypeReference<java.util.List<String>>() {};
        org.axonframework.common.configuration.Component.Identifier<java.util.List<String>> first =
                new org.axonframework.common.configuration.Component.Identifier<java.util.List<String>>(typeRef, "name");
        org.axonframework.common.configuration.Component.Identifier<java.util.List<String>> second = first;

        assertTrue(first.matchesByTypeRef(second));
        assertEquals(first.typeAsClass(), java.util.List.class);
        assertTrue(first.matchesByTypeRef(first));
    }

    @Test
    void exposesTypeAsClassAndEquality() {
        org.axonframework.common.configuration.Component.Identifier<String> first = new org.axonframework.common.configuration.Component.Identifier<>(String.class, "cmp");
        org.axonframework.common.configuration.Component.Identifier<String> second = new org.axonframework.common.configuration.Component.Identifier<>(String.class, "cmp");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }
}