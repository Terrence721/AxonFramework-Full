package org.axonframework.integrationtests;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.eventhandling.GenericEventMessage;
import org.axonframework.messaging.eventhandling.SimpleEventBus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EventBusHandlingSmokeTest {
    @Test
    void publishedEventsAreDeliveredToSubscriber() {
        SimpleEventBus eventBus = new SimpleEventBus();
        AtomicBoolean handled = new AtomicBoolean(false);

        eventBus.subscribe((events, processingContext) -> {
            handled.set(true);
            return CompletableFuture.completedFuture(null);
        });

        eventBus.publish(
                null,
                List.of(new GenericEventMessage(new MessageType("test", "Event", MessageType.DEFAULT_VERSION), "payload", Map.of()))
        ).join();

        assertTrue(handled.get());
    }
}
