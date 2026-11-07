package org.axonframework.integrationtests;

import java.util.List;
import java.util.Map;

import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.eventsourcing.eventstore.MetadataBasedTagResolver;
import org.axonframework.eventsourcing.eventstore.StorageEngineBackedEventStore;
import org.axonframework.eventsourcing.eventstore.inmemory.InMemoryEventStorageEngine;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.eventhandling.GenericEventMessage;
import org.axonframework.messaging.eventhandling.SimpleEventBus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EventPublicationSmokeTest {
    @Test
    void eventsCanBePublishedThroughStore() {
        EventStore store = new StorageEngineBackedEventStore(
                new InMemoryEventStorageEngine(),
                new SimpleEventBus(),
                new MetadataBasedTagResolver("integrationtests"));

        assertDoesNotThrow(() -> store.publish(
                null,
                List.of(new GenericEventMessage(new MessageType("test", "Event", MessageType.DEFAULT_VERSION), "payload", Map.of()))
        ).join());

        assertNotNull(store.latestToken(null).join());
    }
}
