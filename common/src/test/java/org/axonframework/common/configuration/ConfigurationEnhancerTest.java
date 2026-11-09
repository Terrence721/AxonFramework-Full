package org.axonframework.common.configuration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigurationEnhancerTest {

    @Test
    void defaultOrderIsZeroAndEnhanceRuns() {
        ConfigurationEnhancer enhancer = registry -> {
        };

        assertEquals(0, enhancer.order());
    }
}