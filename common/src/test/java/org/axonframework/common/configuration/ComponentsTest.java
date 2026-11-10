package org.axonframework.common.configuration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentsTest {

    @Test
    void putAndRetrieveComponent() {
        Components components = new Components();
        Component<String> component = new InstantiatedComponentDefinition<>(
                new Component.Identifier<>(String.class, "cmp"), "value");

        assertNullReturn(components.put(component));
        assertEquals(component, components.get(new Component.Identifier<>(String.class, "cmp")).orElse(null));
    }

    @Test
    void containsMatchesByTypeWhenNameIsNull() {
        Components components = new Components();
        components.put(new InstantiatedComponentDefinition<>(
                new Component.Identifier<>(String.class, "cmp"), "value"));

        assertTrue(components.contains(new Component.Identifier<>(Object.class, null)));
        assertFalse(components.contains(new Component.Identifier<>(Integer.class, "cmp")));
    }

    @Test
    void computeIfAbsentCreatesComponent() {
        Components components = new Components();
        Component.Identifier<String> identifier = new Component.Identifier<>(String.class, "lazy");

        Component<String> created = components.computeIfAbsent(identifier, () ->
                new InstantiatedComponentDefinition<>(identifier, "lazy-value"));

        assertNotNull(created);
        assertEquals(identifier, created.identifier());
        assertEquals(created, components.get(identifier).orElse(null));
    }

    @Test
    void ambiguousMatchThrows() {
        Components components = new Components();
        components.put(new InstantiatedComponentDefinition<>(new Component.Identifier<>(String.class, null), "one"));
        components.put(new InstantiatedComponentDefinition<>(new Component.Identifier<>(Integer.class, null), 2));

        assertThrows(AmbiguousComponentMatchException.class,
                () -> components.get(new Component.Identifier<>(Object.class, null)));
    }

    private static void assertNullReturn(Object value) {
        org.junit.jupiter.api.Assertions.assertNull(value);
    }
}