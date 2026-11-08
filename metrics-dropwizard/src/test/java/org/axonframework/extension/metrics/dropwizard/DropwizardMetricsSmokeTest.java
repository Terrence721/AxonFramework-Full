package org.axonframework.extension.metrics.dropwizard;

import org.axonframework.messaging.core.GenericMessage;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.eventhandling.EventMessage;
import org.axonframework.messaging.eventhandling.GenericEventMessage;
import org.axonframework.messaging.monitoring.MessageMonitor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DropwizardMetricsSmokeTest {
    @Test
    void capacityMonitorProvidesCapacityMetric() {
        CapacityMonitor monitor = new CapacityMonitor(1, java.util.concurrent.TimeUnit.MINUTES);

        MessageMonitor.MonitorCallback callback =
                monitor.onMessageIngested(new GenericMessage(new MessageType("example", "Message", MessageType.DEFAULT_VERSION), "payload"));
        callback.reportSuccess();

        assertNotNull(monitor.getMetrics().get(io.dropwizard.metrics5.MetricName.build("capacity")));
        assertTrue(monitor.getMetrics().containsKey(io.dropwizard.metrics5.MetricName.build("capacity")));
    }

    @Test
    void timerMonitorBuildsAllTimers() {
        MessageTimerMonitor monitor = MessageTimerMonitor.builder().build();

        MessageMonitor.MonitorCallback callback =
                monitor.onMessageIngested(new GenericMessage(new MessageType("example", "Message", MessageType.DEFAULT_VERSION), "payload"));
        callback.reportSuccess();
        callback.reportIgnored();

        assertNotNull(monitor.getMetrics().get(io.dropwizard.metrics5.MetricName.build("allTimer")));
    }

    @Test
    void eventProcessorLatencyMonitorWritesLatency() {
        EventProcessorLatencyMonitor monitor = new EventProcessorLatencyMonitor();
        EventMessage message = new GenericEventMessage(new MessageType("example", "Event", MessageType.DEFAULT_VERSION), "payload");

        monitor.onMessageIngested(message).reportSuccess();

        assertNotNull(monitor.getMetrics().get(io.dropwizard.metrics5.MetricName.build("latency")));
    }

    @Test
    void payloadWrapperCreatesAndUsesInnerMonitor() {
        PayloadTypeMessageMonitorWrapper<MessageCountingMonitor> wrapper =
                new PayloadTypeMessageMonitorWrapper<>(MessageCountingMonitor::new);

        MessageMonitor.MonitorCallback callback =
                wrapper.onMessageIngested(new GenericMessage(new MessageType("example", "Message", MessageType.DEFAULT_VERSION), "payload"));
        callback.reportSuccess();

        assertNotNull(wrapper.getMetrics());
        assertFalse(wrapper.getMetrics().isEmpty());
    }

    @Test
    void configurationEnhancerLoads() {
        MetricsConfigurationEnhancer enhancer = new MetricsConfigurationEnhancer();
        assertNotNull(enhancer);
    }
}
