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

package org.axonframework.eventsourcing.snapshot.inmemory;

import org.axonframework.eventsourcing.eventstore.GlobalIndexPosition;
import org.axonframework.eventsourcing.snapshot.api.Snapshot;
import org.axonframework.messaging.core.QualifiedName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("deprecation")
class InMemorySnapshotStoreTest {

    @Test
    void storeAndLoadSameSnapshot() throws ExecutionException, InterruptedException {
        InMemorySnapshotStore store = new InMemorySnapshotStore();
        QualifiedName name = new QualifiedName("org", "event");
        Snapshot snapshot = new Snapshot(new GlobalIndexPosition(1), "1.0", "payload", Instant.EPOCH, Map.of());

        store.store(name, "id", snapshot, null).get();
        Snapshot loaded = store.load(name, "id", null).get();

        assertEquals(snapshot, loaded);
    }
}
