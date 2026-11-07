package org.axonframework.integrationtests;

import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.eventsourcing.eventstore.MetadataBasedTagResolver;
import org.axonframework.eventsourcing.eventstore.StorageEngineBackedEventStore;
import org.axonframework.eventsourcing.eventstore.inmemory.InMemoryEventStorageEngine;
import org.axonframework.messaging.eventhandling.SimpleEventBus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class EventSourcingSmokeTest {
    @Test
    void eventStoreModulesLoadTogether() {
        EventStore eventStore = new StorageEngineBackedEventStore(
                new InMemoryEventStorageEngine(),
                new SimpleEventBus(),
                new MetadataBasedTagResolver("integrationtests"));

        assertNotNull(eventStore);
    }
}
