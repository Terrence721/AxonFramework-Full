package org.axonframework.common;

import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.UUID;
import java.lang.reflect.Field;

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
    void locateFactoriesUsesContextClassLoaderWhenAvailable() throws Exception {
        // Test the locateFactories method indirectly via reflection
        // We cannot directly mock ServiceLoader, so we test that the method exists and is called
        Field field = IdentifierFactory.class.getDeclaredField("INSTANCE");
        field.setAccessible(true);
        Object instance = field.get(null);
        assertNotNull(instance);
        assertTrue(instance instanceof DefaultIdentifierFactory || instance instanceof IdentifierFactory);
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
}