package org.axonframework.common;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultIdentifierFactoryTest {

    @Test
    void generateIdentifierReturnsUuidString() {
        String identifier = new DefaultIdentifierFactory().generateIdentifier();
        assertNotNull(identifier);
        assertEquals(identifier, UUID.fromString(identifier).toString());
    }

    @Test
    void generateIdentifierProducesUniqueValues() {
        DefaultIdentifierFactory factory = new DefaultIdentifierFactory();
        String id1 = factory.generateIdentifier();
        String id2 = factory.generateIdentifier();
        assertNotNull(id1);
        assertNotNull(id2);
        // Very unlikely to be equal; test inequality
        assertTrue(!id1.equals(id2));
    }

    @Test
    void constructorIsAccessibleAndCreatesInstance() throws Exception {
        java.lang.reflect.Constructor<DefaultIdentifierFactory> ctor = DefaultIdentifierFactory.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        DefaultIdentifierFactory instance = ctor.newInstance();
        assertNotNull(instance);
        // Ensure generateIdentifier works on the reflected instance
        String identifier = instance.generateIdentifier();
        assertNotNull(identifier);
        assertEquals(identifier, UUID.fromString(identifier).toString());
    }
}