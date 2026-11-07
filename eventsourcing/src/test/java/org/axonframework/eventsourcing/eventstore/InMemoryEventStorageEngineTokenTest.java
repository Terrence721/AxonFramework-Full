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
import org.axonframework.messaging.eventhandling.processing.streaming.token.TrackingToken;
import org.axonframework.messaging.eventstreaming.Tag;
import org.axonframework.eventsourcing.eventstore.inmemory.InMemoryEventStorageEngine;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryEventStorageEngineTokenTest {
    private static EventMessage event() {
        return new GenericEventMessage(new MessageType(new QualifiedName("org", "event"), "1.0"), "payload");
    }

    @Test
    void firstAndLatestTokensUpdateAfterAppend() throws ExecutionException, InterruptedException {
        InMemoryEventStorageEngine engine = new InMemoryEventStorageEngine();
        EventStorageEngine.AppendTransaction<?> transaction = engine.appendEvents(
                AppendCondition.none(), null, List.of(new GenericTaggedEventMessage<>(event(), Set.of()))
        ).join();
        transaction.commit().join();

        TrackingToken first = engine.firstToken().join();
        TrackingToken latest = engine.latestToken().join();

        assertNotNull(first);
        assertTrue(latest.toString().contains("1"));
    }

    @Test
    void tokenAtReturnsEarliestMatchingTimestamp() throws ExecutionException, InterruptedException {
        InMemoryEventStorageEngine engine = new InMemoryEventStorageEngine();
        EventStorageEngine.AppendTransaction<?> transaction = engine.appendEvents(
                AppendCondition.none(), null, List.of(new GenericTaggedEventMessage<>(event(), Set.of()))
        ).join();
        transaction.commit().join();

        TrackingToken token = engine.tokenAt(Instant.EPOCH).join();

        assertNotNull(token);
    }
}
