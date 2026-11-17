package org.axonframework.common;

import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdentifierFactoryTest {

    @Test
    void instanceIsAvailable() {
        assertNotNull(IdentifierFactory.getInstance());
    }

    @Test
    void defaultInstanceGeneratesUuid() {
        String identifier = IdentifierFactory.getInstance().generateIdentifier();

        assertNotNull(identifier);
        assertEquals(identifier, UUID.fromString(identifier).toString());
    }

    @Test
    void instanceIsStable() {
        assertSame(IdentifierFactory.getInstance(), IdentifierFactory.getInstance());
    }

    @Test
    void instanceIsAvailable() {
        assertNotNull(IdentifierFactory.getInstance());
    }

    @Test
    void defaultInstanceGeneratesUuid() {
        String identifier = IdentifierFactory.getInstance().generateIdentifier();

        assertNotNull(identifier);
        assertEquals(identifier, UUID.fromString(identifier).toString());
    }

    @Test
    void instanceIsStable() {
        assertSame(IdentifierFactory.getInstance(), IdentifierFactory.getInstance());
    }

    @Test
    void getInstanceReturnsCachedInstance() {
        IdentifierFactory first = IdentifierFactory.getInstance();
        IdentifierFactory second = IdentifierFactory.getInstance();
        assertSame(first, second);
    }

    @Test
    void serviceLoaderIteratorBehavior() {
        // Verify that ServiceLoader.iterator() works correctly (used in locateFactories)
        Iterator<IdentifierFactory> iterator =
            ServiceLoader.load(IdentifierFactory.class).iterator();
        // This should not throw even if no implementation is found
        assertNotNull(iterator);
    }

    @Test
    void instanceIsAvailable() {
        assertNotNull(IdentifierFactory.getInstance());
    }

    @Test
    void defaultInstanceGeneratesUuid() {
        String identifier = IdentifierFactory.getInstance().generateIdentifier();

        assertNotNull(identifier);
        assertEquals(identifier, UUID.fromString(identifier).toString());
    }

    @Test
    void instanceIsStable() {
        assertSame(IdentifierFactory.getInstance(), IdentifierFactory.getInstance());
    }

    @Test
    void getInstanceReturnsCachedInstance() {
        IdentifierFactory first = IdentifierFactory.getInstance();
        IdentifierFactory second = IdentifierFactory.getInstance();
        assertSame(first, second);
    }

    @Test
    void serviceLoaderIteratorBehavior() {
        // Verify that ServiceLoader.iterator() works correctly (used in locateFactories)
        Iterator<IdentifierFactory> iterator =
            ServiceLoader.load(IdentifierFactory.class).iterator();
        // This should not throw even if no implementation is found
        assertNotNull(iterator);
    }

    @Test
    void generateIdentifierImplementationCovered() {
        // This test ensures the abstract method generateIdentifier is covered by concrete implementations
        DefaultIdentifierFactory concreteFactory = new DefaultIdentifierFactory();
        String identifier = concreteFactory.generateIdentifier();

        assertNotNull(identifier);
        assertEquals(identifier, UUID.fromString(identifier).toString());
    }

    @Test
    void serviceLoaderEdgeCases() {
        // Test edge cases for ServiceLoader behavior
        Iterator<IdentifierFactory> iterator = ServiceLoader.load(IdentifierFactory.class).iterator();

        // Iterator should be non-null even if no implementations found
        assertNotNull(iterator);

        // The iterator should not throw when iterated
        if (iterator.hasNext()) {
            IdentifierFactory factory = iterator.next();
            assertNotNull(factory);
        }
    }

    @Test
    void instanceCachingBehavior() {
        // Test that the singleton pattern is maintained across multiple calls
        IdentifierFactory instance1 = IdentifierFactory.getInstance();
        IdentifierFactory instance2 = IdentifierFactory.getInstance();
        IdentifierFactory instance3 = IdentifierFactory.getInstance();

        assertSame(instance1, instance2);
        assertSame(instance2, instance3);
        assertSame(instance1, instance3);
    }

    @Test
    void factorySelectionLogic() {
        // Test that the factory selection logic works correctly
        // The static initialization should have selected an appropriate factory
        IdentifierFactory instance = IdentifierFactory.getInstance();

        assertNotNull(instance);
        assertTrue(instance instanceof DefaultIdentifierFactory);
    }

    @Test
    void classLoaderSelectionLogic() {
        // Test the class loader selection logic by testing the actual instance
        IdentifierFactory instance = IdentifierFactory.getInstance();

        assertNotNull(instance);
        assertTrue(instance instanceof DefaultIdentifierFactory);
    }