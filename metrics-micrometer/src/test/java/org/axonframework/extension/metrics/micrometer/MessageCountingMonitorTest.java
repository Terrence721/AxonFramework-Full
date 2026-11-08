package org.axonframework.extension.metrics.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.axonframework.messaging.core.GenericMessage;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.monitoring.MessageMonitor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageCountingMonitorTest {
    @Test
    void countsIngestedSuccessFailedAndIgnoredMessages() {
        MeterRegistry registry = new SimpleMeterRegistry();
        MessageCountingMonitor monitor = MessageCountingMonitor.buildMonitor("test", registry);
        GenericMessage message = new GenericMessage(new MessageType("example", "Message", MessageType.DEFAULT_VERSION), "payload");

        MessageMonitor.MonitorCallback success = monitor.onMessageIngested(message);
        success.reportSuccess();
        success.reportIgnored();

        MessageMonitor.MonitorCallback failure = monitor.onMessageIngested(message);
        failure.reportFailure(new IllegalStateException("boom"));

                assertEquals(2, registry.counter("test.messageCounter.ingested").count());
        assertEquals(2, registry.counter("test.messageCounter.processed").count());
        assertEquals(1, registry.counter("test.messageCounter.success").count());
        assertEquals(1, registry.counter("test.messageCounter.failure").count());
        assertEquals(1, registry.counter("test.messageCounter.ignored").count());
    }
}
