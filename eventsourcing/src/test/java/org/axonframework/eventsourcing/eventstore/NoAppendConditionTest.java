package org.axonframework.eventsourcing.eventstore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NoAppendConditionTest {
    @Test
    void hasInfinityMarker() {
        NoAppendCondition condition = NoAppendCondition.INSTANCE;

        assertEquals(ConsistencyMarker.INFINITY, condition.consistencyMarker());
    }
}
