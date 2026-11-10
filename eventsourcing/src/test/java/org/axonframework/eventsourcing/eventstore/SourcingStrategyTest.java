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

import org.axonframework.messaging.core.QualifiedName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SourcingStrategyTest {
    @Test
    void mergingAbsoluteReturnsMinimum() {
        SourcingStrategy merged = new SourcingStrategy.Absolute(Position.START).merge(new SourcingStrategy.Absolute(new GlobalIndexPosition(0)));

        SourcingStrategy.Absolute absolute = (SourcingStrategy.Absolute) merged;
        assertEquals(Position.START, absolute.position());
    }

    @Test
    void mergingTwoSnapshotsThrows() {
        QualifiedName name = new QualifiedName("org", "event");
        SourcingStrategy.Snapshot first = new SourcingStrategy.Snapshot(name, "id", null);
        SourcingStrategy second = new SourcingStrategy.Snapshot(name, "id", null);

        assertThrows(UnsupportedOperationException.class, () -> first.merge(second));
    }
}
