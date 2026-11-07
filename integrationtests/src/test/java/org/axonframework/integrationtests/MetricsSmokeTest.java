package org.axonframework.integrationtests;

import org.axonframework.extension.metrics.micrometer.MessageCountingMonitor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class MetricsSmokeTest {
    @Test
    void metricsModuleLoads() {
        assertNotNull(MessageCountingMonitor.class);
    }
}
