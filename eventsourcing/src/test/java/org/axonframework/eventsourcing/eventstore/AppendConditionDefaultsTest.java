package org.axonframework.eventsourcing.eventstore;

import org.axonframework.messaging.eventstreaming.EventCriteria;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AppendConditionDefaultsTest {
    @Test
    void withCriteriaCreatesOriginMarker() {
        AppendCondition condition = AppendCondition.withCriteria(EventCriteria.havingAnyTag());

        assertEquals(ConsistencyMarker.ORIGIN, condition.consistencyMarker());
        assertNotNull(condition.withMarker(ConsistencyMarker.INFINITY));
    }
}
