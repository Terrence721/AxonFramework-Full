package org.axonframework.extension.metrics.dropwizard;

import io.dropwizard.metrics5.Counter;
import io.dropwizard.metrics5.MetricName;
import org.axonframework.messaging.core.GenericMessage;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.monitoring.MessageMonitor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageCountingMonitorTest {
    @Test
    void countersTrackIngestedSuccessFailedAndIgnoredMessages() {
        MessageCountingMonitor monitor = new MessageCountingMonitor();
        GenericMessage message = new GenericMessage(new MessageType("example", "Message", MessageType.DEFAULT_VERSION), "payload");

        MessageMonitor.MonitorCallback success = monitor.onMessageIngested(message);
        success.reportSuccess();
        success.reportIgnored();

        MessageMonitor.MonitorCallback failure = monitor.onMessageIngested(message);
        failure.reportFailure(new IllegalStateException("boom"));

        assertEquals(2.0, ((Counter) monitor.getMetrics().get(MetricName.build("ingestedCounter"))).getCount());
        assertEquals(2.0, ((Counter) monitor.getMetrics().get(MetricName.build("processedCounter"))).getCount());
        assertEquals(1.0, ((Counter) monitor.getMetrics().get(MetricName.build("successCounter"))).getCount());
        assertEquals(1.0, ((Counter) monitor.getMetrics().get(MetricName.build("failureCounter"))).getCount());
        assertEquals(1.0, ((Counter) monitor.getMetrics().get(MetricName.build("ignoredCounter"))).getCount());
    }
}
