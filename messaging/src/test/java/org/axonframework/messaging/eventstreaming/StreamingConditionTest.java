/*
 * Copyright (c) 2026. Terrence Daniels
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

package org.axonframework.messaging.eventstreaming;

import org.axonframework.messaging.core.QualifiedName;
import org.axonframework.messaging.eventhandling.processing.streaming.token.GlobalSequenceTrackingToken;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link StreamingCondition} (both its {@code startingFrom} and {@code conditionFor}
 * variants) and the default {@link EventsCondition#matches(QualifiedName, Set)}.
 */
class StreamingConditionTest {

    private static final GlobalSequenceTrackingToken POSITION = new GlobalSequenceTrackingToken(1337);
    private static final QualifiedName TYPE = new QualifiedName("test.Type");
    private static final Tag TAG_A = Tag.of("a", "1");
    private static final Tag TAG_B = Tag.of("b", "2");

    // --- events condition ---

    @Test
    void anEventsConditionMatchesThroughItsCriteria() {
        EventsCondition testSubject = () -> EventCriteria.havingTags(TAG_A);

        assertTrue(testSubject.matches(TYPE, Set.of(TAG_A)));
        assertFalse(testSubject.matches(TYPE, Set.of(TAG_B)));
    }

    // --- starting from ---

    @Test
    void startingFromHasThePositionAndMatchesAnyEvent() {
        StreamingCondition testSubject = StreamingCondition.startingFrom(POSITION);

        assertSame(POSITION, testSubject.position());
        assertEquals(EventCriteria.havingAnyTag(), testSubject.criteria());
        assertTrue(testSubject.matches(TYPE, Set.of()));
    }

    @Test
    void startingFromAcceptsANullPosition() {
        assertNull(StreamingCondition.startingFrom(null).position());
    }

    @Test
    void withCriteriaOnStartingFromKeepsThePositionAndUsesTheGivenCriteria() {
        EventCriteria criteria = EventCriteria.havingTags(TAG_A);

        StreamingCondition result = StreamingCondition.startingFrom(POSITION).withCriteria(criteria);

        assertSame(POSITION, result.position());
        assertEquals(criteria, result.criteria());
    }

    @Test
    void orOnStartingFromReplacesTheMatchAnythingCriteriaWithTheGivenCriteria() {
        EventCriteria criteria = EventCriteria.havingTags(TAG_A);

        StreamingCondition result = StreamingCondition.startingFrom(POSITION).or(criteria);

        assertSame(POSITION, result.position());
        assertEquals(criteria, result.criteria());
        assertFalse(result.matches(TYPE, Set.of(TAG_B)));
    }

    @Test
    void startingFromWithoutAPositionCannotTakeCriteria() {
        StreamingCondition testSubject = StreamingCondition.startingFrom(null);
        EventCriteria criteria = EventCriteria.havingTags(TAG_A);

        assertEquals("The position may not be null when adding criteria to it",
                     assertThrows(IllegalArgumentException.class, () -> testSubject.withCriteria(criteria))
                             .getMessage());
        assertEquals("The position may not be null when adding criteria to it",
                     assertThrows(IllegalArgumentException.class, () -> testSubject.or(criteria)).getMessage());
    }

    // --- condition for ---

    @Test
    void conditionForHasThePositionAndTheCriteria() {
        EventCriteria criteria = EventCriteria.havingTags(TAG_A);

        StreamingCondition testSubject = StreamingCondition.conditionFor(POSITION, criteria);

        assertSame(POSITION, testSubject.position());
        assertSame(criteria, testSubject.criteria());
        assertTrue(testSubject.matches(TYPE, Set.of(TAG_A)));
        assertFalse(testSubject.matches(TYPE, Set.of()));
    }

    @Test
    void conditionForRejectsANullPositionOrCriteria() {
        assertEquals("The position cannot be null", assertThrows(NullPointerException.class,
                () -> StreamingCondition.conditionFor(null, EventCriteria.havingAnyTag())).getMessage());
        assertEquals("The EventCriteria cannot be null", assertThrows(NullPointerException.class,
                () -> StreamingCondition.conditionFor(POSITION, null)).getMessage());
    }

    @Test
    void withCriteriaReplacesTheCriteriaAndKeepsThePosition() {
        StreamingCondition testSubject = StreamingCondition.conditionFor(POSITION, EventCriteria.havingTags(TAG_A));
        EventCriteria replacement = EventCriteria.havingTags(TAG_B);

        StreamingCondition result = testSubject.withCriteria(replacement);

        assertSame(POSITION, result.position());
        assertSame(replacement, result.criteria());
        assertEquals(EventCriteria.havingTags(TAG_A), testSubject.criteria());
    }

    @Test
    void orCombinesTheNewCriteriaWithTheExistingOnesAndKeepsThePosition() {
        StreamingCondition testSubject = StreamingCondition.conditionFor(POSITION, EventCriteria.havingTags(TAG_A));

        StreamingCondition result = testSubject.or(EventCriteria.havingTags(TAG_B));

        assertSame(POSITION, result.position());
        assertTrue(result.matches(TYPE, Set.of(TAG_A)));
        assertTrue(result.matches(TYPE, Set.of(TAG_B)));
        assertFalse(result.matches(TYPE, Set.of()));
    }

    @Test
    void orWithMatchAnythingMakesTheConditionMatchEverything() {
        StreamingCondition testSubject = StreamingCondition.conditionFor(POSITION, EventCriteria.havingTags(TAG_A));

        StreamingCondition result = testSubject.or(EventCriteria.havingAnyTag());

        assertEquals(EventCriteria.havingAnyTag(), result.criteria());
        assertTrue(result.matches(TYPE, Set.of()));
    }

    @Test
    void conditionsHaveValueEquality() {
        EventCriteria criteria = EventCriteria.havingTags(TAG_A);

        assertEquals(StreamingCondition.conditionFor(POSITION, criteria),
                     StreamingCondition.conditionFor(new GlobalSequenceTrackingToken(1337), criteria));
        assertNotEquals(StreamingCondition.conditionFor(POSITION, criteria),
                        StreamingCondition.conditionFor(new GlobalSequenceTrackingToken(1), criteria));
        assertEquals(StreamingCondition.startingFrom(POSITION), StreamingCondition.startingFrom(POSITION));
        assertNotEquals(StreamingCondition.startingFrom(POSITION),
                        StreamingCondition.conditionFor(POSITION, EventCriteria.havingAnyTag()));
    }
}
