package org.axonframework.integrationtests;

import org.axonframework.messaging.core.Metadata;
import org.axonframework.messaging.eventhandling.EventBus;
import org.axonframework.messaging.eventhandling.SimpleEventBus;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.eventsourcing.eventstore.StorageEngineBackedEventStore;
import org.axonframework.eventsourcing.EventSourcingRepository;
import org.axonframework.eventsourcing.eventstore.inmemory.InMemoryEventStorageEngine;
import org.axonframework.eventsourcing.eventstore.MetadataBasedTagResolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class IntegrationModuleSmokeTest {
    @Test
    void modulesCanBeLoadedTogether() {
        Metadata metadata = Metadata.emptyInstance();
        EventBus eventBus = new SimpleEventBus();
        EventStore eventStore = new StorageEngineBackedEventStore(new InMemoryEventStorageEngine(), eventBus, new MetadataBasedTagResolver("integrationtests"));

        assertNotNull(eventStore);
        assertNotNull(metadata);
    }
}
