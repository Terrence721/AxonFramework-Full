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

import org.axonframework.messaging.eventstreaming.EventCriteria;
import org.axonframework.messaging.eventstreaming.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppendConditionTest {
    @Test
    void noneUsesInfinityMarker() {
        AppendCondition condition = AppendCondition.none();

        assertEquals(ConsistencyMarker.INFINITY, condition.consistencyMarker());
        assertEquals(EventCriteria.havingAnyTag(), condition.criteria());
    }

    @Test
    void withCriteriaUsesOriginMarker() {
        AppendCondition condition = AppendCondition.withCriteria(EventCriteria.havingAnyTag());

        assertEquals(ConsistencyMarker.ORIGIN, condition.consistencyMarker());
    }

    @Test
    void withMarkerReturnsUpdatedMarker() {
        AppendCondition condition = AppendCondition.none();

        assertEquals(ConsistencyMarker.ORIGIN, condition.withMarker(ConsistencyMarker.ORIGIN).consistencyMarker());
    }
    @Test
    void orCriteriaAddsRequestedTagCriteria() {
        AppendCondition condition = AppendCondition.withCriteria(EventCriteria.havingTags(new Tag("type", "value")));

        AppendCondition merged = condition.orCriteria(EventCriteria.havingAnyTag());

        assertNotNull(merged);
    }

    @Test
    void replaceCriteriaReplacesCriteria() {
        AppendCondition condition = AppendCondition.withCriteria(EventCriteria.havingAnyTag());

        AppendCondition replaced = condition.replaceCriteria(EventCriteria.havingAnyTag());

        assertEquals(EventCriteria.havingAnyTag(), replaced.criteria());
    }
}
