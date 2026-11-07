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

import org.axonframework.messaging.eventhandling.EventMessage;
import org.axonframework.messaging.eventhandling.GenericEventMessage;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.QualifiedName;
import org.junit.jupiter.api.Test;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotPolicyTest {
    @Test
    void afterEventsTriggersWhenAppliedEventsExceedThreshold() {
        SnapshotPolicy policy = SnapshotPolicy.afterEvents(1);

        assertTrue(policy.shouldSnapshot(new EvolutionResult(2, Duration.ZERO)));
        assertFalse(policy.shouldSnapshot(new EvolutionResult(1, Duration.ZERO)));
    }

    @Test
    void whenSourcingTimeExceedsTriggers() {
        SnapshotPolicy policy = SnapshotPolicy.whenSourcingTimeExceeds(Duration.ofSeconds(1));

        assertTrue(policy.shouldSnapshot(new EvolutionResult(0, Duration.ofSeconds(2))));
        assertFalse(policy.shouldSnapshot(new EvolutionResult(0, Duration.ofMillis(500))));
    }

    @Test
    void orCombinesTwoPolicies() {
        SnapshotPolicy policy = SnapshotPolicy.afterEvents(1).or(SnapshotPolicy.whenSourcingTimeExceeds(Duration.ofSeconds(1)));

        assertTrue(policy.shouldSnapshot(new EvolutionResult(2, Duration.ofMillis(100))));
        assertTrue(policy.shouldSnapshot(new EvolutionResult(0, Duration.ofSeconds(2))));
    }

    @Test
    void whenEventMatchesUsesEventPredicate() {
        EventMessage event = new GenericEventMessage(new MessageType(new QualifiedName("org", "event"), "1.0"), "payload");
        SnapshotPolicy policy = SnapshotPolicy.whenEventMatches(e -> true);

        assertTrue(policy.shouldSnapshot(event));
        assertFalse(policy.shouldSnapshot(new EvolutionResult(1, Duration.ZERO)));
    }
}
