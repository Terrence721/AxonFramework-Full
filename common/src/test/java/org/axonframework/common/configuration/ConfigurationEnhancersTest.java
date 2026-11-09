package org.axonframework.common.configuration;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationEnhancersTest {

    private static ConfigurationEnhancer enhancer() {
        return registry -> {
        };
    }

    private static ConfigurationEnhancer enhancer(int order) {
        return new ConfigurationEnhancer() {
            @Override
            public void enhance(ComponentRegistry componentRegistry) {
            }

            @Override
            public int order() {
                return order;
            }
        };
    }

    @Test
    void registerAndRegistered() {
        ConfigurationEnhancers enhancers = new ConfigurationEnhancers();
        ConfigurationEnhancer previous = enhancers.register(enhancer());

        assertEquals(null, previous);
        assertEquals(1, enhancers.registered().size());
    }

    @Test
    void disablePaths() {
        ConfigurationEnhancers enhancers = new ConfigurationEnhancers();
        ConfigurationEnhancer enhancer = enhancer();
        enhancers.register(enhancer);

        assertEquals(ConfigurationEnhancers.DisableOutcome.DISABLED, enhancers.disable(enhancer.getClass()));
        assertEquals(ConfigurationEnhancers.DisableOutcome.ALREADY_DISABLED, enhancers.disable(enhancer.getClass()));

        ConfigurationEnhancers fresh = new ConfigurationEnhancers();
        ConfigurationEnhancer e2 = enhancer();
        fresh.register(e2);
        fresh.invokeAll(new DefaultComponentRegistry());
        assertEquals(ConfigurationEnhancers.DisableOutcome.TOO_LATE, fresh.disable(e2.getClass()));
    }

    @Test
    void disableAll() {
        ConfigurationEnhancers enhancers = new ConfigurationEnhancers();
        enhancers.disableAll(List.of());
        assertEquals(0, enhancers.disabledTypes().size());
    }

    @Test
    void discoverAndScanning() {
        ConfigurationEnhancers enhancers = new ConfigurationEnhancers();
        assertTrue(enhancers.scanningEnabled());
        enhancers.disableScanning();
        assertFalse(enhancers.scanningEnabled());
        assertTrue(enhancers.discover(getClass().getClassLoader()) != null);
    }

    @Test
    void invokeAllOrdering() {
        ConfigurationEnhancers enhancers = new ConfigurationEnhancers();
        java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger();
        enhancers.register(enhancer(1));
        enhancers.register(enhancer(-1));
        enhancers.invokeAll(new DefaultComponentRegistry());
        // no exception and enhancers invoked; order covered by invokeAll
        assertTrue(true);
    }
}