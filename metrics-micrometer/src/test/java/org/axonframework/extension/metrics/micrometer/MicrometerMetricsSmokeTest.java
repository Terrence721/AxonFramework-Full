package org.axonframework.extension.metrics.micrometer;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.axonframework.messaging.core.GenericMessage;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.eventhandling.GenericEventMessage;
import org.axonframework.messaging.monitoring.MessageMonitor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MicrometerMetricsSmokeTest {
    @Test
    void capacityMonitorRegistersGauge() {
        io.micrometer.core.instrument.MeterRegistry registry = new SimpleMeterRegistry();
        CapacityMonitor monitor = CapacityMonitor.buildMonitor("capacity.", registry, 10, java.util.concurrent.TimeUnit.MINUTES);

        MessageMonitor.MonitorCallback callback =
                monitor.onMessageIngested(new GenericMessage(new MessageType("example", "Message", MessageType.DEFAULT_VERSION), "payload"));
        callback.reportSuccess();

        assertFalse(registry.getMeters().isEmpty());
    }

    @Test
    void messageTimerMonitorRegistersAllTimers() {
        io.micrometer.core.instrument.MeterRegistry registry = new SimpleMeterRegistry();
        MessageTimerMonitor monitor = MessageTimerMonitor.builder().meterNamePrefix("timer.").meterRegistry(registry).build();

        MessageMonitor.MonitorCallback callback =
                monitor.onMessageIngested(new GenericMessage(new MessageType("example", "Message", MessageType.DEFAULT_VERSION), "payload"));
        callback.reportFailure(new IllegalStateException("boom"));
        callback.reportIgnored();

        assertTrue(registry.getMeters().size() >= 4);
    }

    @Test
    void eventProcessorLatencyMonitorRegistersLatencyGauge() {
        io.micrometer.core.instrument.MeterRegistry registry = new SimpleMeterRegistry();
        EventProcessorLatencyMonitor monitor = EventProcessorLatencyMonitor.builder()
                .meterNamePrefix("processor.")
                .meterRegistry(registry)
                .build();

        monitor.onMessageIngested(new GenericEventMessage(new MessageType("example", "Event", MessageType.DEFAULT_VERSION), "payload"))
                .reportSuccess();

        assertFalse(registry.getMeters().isEmpty());
    }

    @Test
    void tagsUtilProvidesFunctions() {
        assertNotNull(TagsUtil.MESSAGE_TYPE_TAGGER);
        assertNotNull(TagsUtil.METADATA_TAGGER);
        assertNotNull(TagsUtil.NO_OP);
        assertNotNull(TagsUtil.processorNameTag("processor"));
        assertNotNull(TagsUtil.processorTags("processor"));
    }

    @Test
    void configurationEnhancerStoresOrder() {
        MetricsConfigurationEnhancer enhancer = new MetricsConfigurationEnhancer(new SimpleMeterRegistry());
        assertNotNull(enhancer);
        assertEquals(-1024, enhancer.order());
    }
}
