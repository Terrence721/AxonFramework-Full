/*
 * Copyright (c) 2010-2026. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations
 * under the License.
 */

package org.axonframework.eventsourcing.eventstore;

import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.QualifiedName;
import org.axonframework.messaging.eventhandling.EventMessage;
import org.axonframework.messaging.eventhandling.GenericEventMessage;
import org.axonframework.messaging.eventstreaming.EventCriteria;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.eventsourcing.eventstore.inmemory.InMemoryEventStorageEngine;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InMemoryEventStorageEngineTest {

    @Test
    void appendAndSourceReturnsEvent() {
        InMemoryEventStorageEngine engine = new InMemoryEventStorageEngine();
        EventMessage event = new GenericEventMessage("id", new MessageType(new QualifiedName("org", "TestEvent"), "1.0"), "payload", Map.of(), Instant.EPOCH);

        EventStorageEngine.AppendTransaction<?> transaction = engine.appendEvents(
                AppendCondition.none(), null, List.of(new GenericTaggedEventMessage<>(event, Set.of()))
        ).join();
        transaction.commit().join();

        MessageStream<EventMessage> source = engine.source(
                SourcingCondition.conditionFor(Position.START, EventCriteria.havingAnyTag()), null);
        assertEquals("payload", source.next().get().message().payload());
    }
}
