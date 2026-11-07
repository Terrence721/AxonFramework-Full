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

package org.axonframework.eventsourcing.snapshot.api;

import org.axonframework.eventsourcing.eventstore.Position;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SnapshotTest {
    @Test
    void constructsAndCopiesPayload() {
        Snapshot snapshot = new Snapshot(Position.START, "1.0", "payload", Instant.EPOCH, Map.of());
        Snapshot copy = snapshot.payload("new");

        assertEquals("new", copy.payload());
        assertEquals("payload", snapshot.payload());
    }

    @Test
    void rejectsNullPosition() {
        assertThrows(NullPointerException.class, () -> new Snapshot(null, "1.0", "payload", Instant.EPOCH, Map.of()));
    }
}
