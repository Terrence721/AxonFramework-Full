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

import org.axonframework.messaging.core.MessageTypeResolver;
import org.axonframework.messaging.core.QualifiedName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link EventCriteria} DSL: tag and type criteria, {@code or} combinations, flattening and
 * {@code mapEachCriterion}.
 */
class EventCriteriaTest {

    private static final QualifiedName ONE = new QualifiedName("test.One");
    private static final QualifiedName TWO = new QualifiedName("test.Two");
    private static final QualifiedName THREE = new QualifiedName("test.Three");
    private static final Tag TAG_A = Tag.of("a", "1");
    private static final Tag TAG_B = Tag.of("b", "2");

    // --- tags ---

    @Test
    void criteriaOnTagsMatchesEventsHavingAllOfThoseTags() {
        EventCriteria testSubject = EventCriteria.havingTags(TAG_A, TAG_B);

        assertTrue(testSubject.matches(ONE, Set.of(TAG_A, TAG_B)));
        assertTrue(testSubject.matches(TWO, Set.of(TAG_A, TAG_B, Tag.of("c", "3"))));
        assertFalse(testSubject.matches(ONE, Set.of(TAG_A)));
        assertFalse(testSubject.matches(ONE, Set.of()));
    }

    @Test
    void tagCriteriaCanBeGivenAsKeyValuePairs() {
        EventCriteria testSubject = EventCriteria.havingTags("a", "1", "b", "2");

        assertTrue(testSubject.matches(ONE, Set.of(TAG_A, TAG_B)));
        assertFalse(testSubject.matches(ONE, Set.of(TAG_A)));
    }

    @Test
    void keyValuePairsMustBeComplete() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> EventCriteria.havingTags("a", "1", "b"));

        assertEquals("Tags must be in pairs of key and value", exception.getMessage());
    }

    @Test
    void noTagsAtAllIsTheSameAsAnyTag() {
        assertSame(EventCriteria.havingAnyTag(), EventCriteria.havingTags(Set.of()));
        assertSame(EventCriteria.havingAnyTag(), EventCriteria.havingTags(new Tag[0]));
        assertSame(EventCriteria.havingAnyTag(), EventCriteria.havingTags(new String[0]));
    }

    @Test
    void tagCriteriaIgnoreTheTypeOfTheEvent() {
        EventCriteria testSubject = EventCriteria.havingTags(TAG_A);

        assertTrue(testSubject.matches(ONE, Set.of(TAG_A)));
        assertTrue(testSubject.matches(THREE, Set.of(TAG_A)));
    }

    @Test
    void tagCriteriaHaveCriteriaAndFlattenToThemselves() {
        EventCriteria testSubject = EventCriteria.havingTags(TAG_A, TAG_B);

        assertTrue(testSubject.hasCriteria());
        assertEquals(1, testSubject.flatten().size());
        EventCriterion criterion = testSubject.flatten().iterator().next();
        assertEquals(Set.of(TAG_A, TAG_B), criterion.tags());
        assertTrue(criterion.types().isEmpty());
    }

    @Test
    void criteriaCopyTheTagsTheyAreGiven() {
        java.util.HashSet<Tag> tags = new java.util.HashSet<>(Set.of(TAG_A));
        EventCriteria testSubject = EventCriteria.havingTags(tags);
        tags.add(TAG_B);

        assertTrue(testSubject.matches(ONE, Set.of(TAG_A)));
    }

    // --- types ---

    @Test
    void restrictingToTypesMatchesOnlyThoseTypesWithTheTags() {
        EventCriteria testSubject = EventCriteria.havingTags(TAG_A).andBeingOneOfTypes(ONE, TWO);

        assertTrue(testSubject.matches(ONE, Set.of(TAG_A)));
        assertTrue(testSubject.matches(TWO, Set.of(TAG_A, TAG_B)));
        assertFalse(testSubject.matches(THREE, Set.of(TAG_A)));
        assertFalse(testSubject.matches(ONE, Set.of()));
    }

    @Test
    void typesCanBeGivenAsNames() {
        EventCriteria testSubject = EventCriteria.havingTags(TAG_A).andBeingOneOfTypes("test.One", "test.Two");

        assertTrue(testSubject.matches(ONE, Set.of(TAG_A)));
        assertFalse(testSubject.matches(THREE, Set.of(TAG_A)));
    }

    @Test
    void typesCanBeResolvedFromClasses() {
        MessageTypeResolver resolver = payloadType -> Optional.of(
                new org.axonframework.messaging.core.MessageType(payloadType.getName()));

        EventCriteria testSubject = EventCriteria.havingTags(TAG_A).andBeingOneOfTypes(resolver, String.class);

        assertTrue(testSubject.matches(new QualifiedName("java.lang.String"), Set.of(TAG_A)));
        assertFalse(testSubject.matches(ONE, Set.of(TAG_A)));
    }

    @Test
    void beingOfAnyTypeKeepsTheCriteriaAsIs() {
        org.axonframework.messaging.eventstreaming.EventTypeRestrictableEventCriteria tagCriteria =
                EventCriteria.havingTags(TAG_A);

        assertSame(tagCriteria, tagCriteria.andBeingOfAnyType());
    }

    @Test
    void typeCriteriaWithoutTagsStillHaveCriteria() {
        EventCriteria testSubject = EventCriteria.havingAnyTag().andBeingOneOfTypes(ONE);

        assertTrue(testSubject.hasCriteria());
        assertTrue(testSubject.matches(ONE, Set.of()));
        assertFalse(testSubject.matches(TWO, Set.of(TAG_A)));
        EventCriterion criterion = testSubject.flatten().iterator().next();
        assertEquals(Set.of(ONE), criterion.types());
        assertTrue(criterion.tags().isEmpty());
    }

    @Test
    void anEmptyTypeRestrictionMatchesAnyTypeAndHasNoCriteriaOfItsOwn() {
        EventCriteria testSubject = EventCriteria.havingAnyTag().andBeingOneOfTypes(Set.of());

        assertTrue(testSubject.matches(ONE, Set.of()));
        assertFalse(testSubject.hasCriteria());
        assertTrue(testSubject.flatten().isEmpty());
    }

    // --- or ---

    @Test
    void orMatchesEventsMatchingEitherCriteria() {
        EventCriteria testSubject = EventCriteria.havingTags(TAG_A).or(EventCriteria.havingTags(TAG_B));

        assertTrue(testSubject.matches(ONE, Set.of(TAG_A)));
        assertTrue(testSubject.matches(ONE, Set.of(TAG_B)));
        assertFalse(testSubject.matches(ONE, Set.of(Tag.of("c", "3"))));
    }

    @Test
    void orOfTheSameCriteriaIsThatCriteria() {
        EventCriteria tagCriteria = EventCriteria.havingTags(TAG_A);
        EventCriteria typeCriteria = tagCriteria.or(EventCriteria.havingTags(TAG_B)).or(tagCriteria);

        assertSame(tagCriteria, tagCriteria.or(EventCriteria.havingTags(TAG_A)));
        assertTrue(typeCriteria.matches(ONE, Set.of(TAG_B)));
        assertEquals(2, typeCriteria.flatten().size());
    }

    @Test
    void orWithAnEventTypeCriterionOfTheSameKindReturnsTheFirst() {
        EventCriteria first = EventCriteria.havingTags(TAG_A).andBeingOneOfTypes(ONE);
        EventCriteria same = EventCriteria.havingTags(TAG_A).andBeingOneOfTypes(ONE);

        assertSame(first, first.or(same));
    }

    @Test
    void orWithMatchAnythingMatchesAnything() {
        assertSame(EventCriteria.havingAnyTag(), EventCriteria.havingTags(TAG_A).or(EventCriteria.havingAnyTag()));
        assertSame(EventCriteria.havingAnyTag(),
                   EventCriteria.havingTags(TAG_A).andBeingOneOfTypes(ONE).or(EventCriteria.havingAnyTag()));
        assertSame(EventCriteria.havingAnyTag(),
                   EventCriteria.either(EventCriteria.havingTags(TAG_A), EventCriteria.havingTags(TAG_B))
                                .or(EventCriteria.havingAnyTag()));
    }

    @Test
    void orIsFlattenedAcrossNestedOrs() {
        EventCriteria ab = EventCriteria.havingTags(TAG_A).or(EventCriteria.havingTags(TAG_B));

        EventCriteria result = EventCriteria.havingTags(Tag.of("c", "3")).or(ab);

        assertEquals(3, result.flatten().size());
        assertTrue(result.matches(ONE, Set.of(TAG_A)));
        assertTrue(result.matches(ONE, Set.of(TAG_B)));
        assertTrue(result.matches(ONE, Set.of(Tag.of("c", "3"))));
    }

    @Test
    void anOrCombinedWithAnotherOrGathersAllOfTheirCriteria() {
        EventCriteria first = EventCriteria.either(EventCriteria.havingTags(TAG_A), EventCriteria.havingTags(TAG_B));
        EventCriteria second = EventCriteria.either(EventCriteria.havingTags(Tag.of("c", "3")),
                                                    EventCriteria.havingTags(Tag.of("d", "4")));

        EventCriteria result = first.or(second);

        assertEquals(4, result.flatten().size());
    }

    @Test
    void anOrCombinedWithAnEqualOrIsThatOr() {
        EventCriteria first = EventCriteria.either(EventCriteria.havingTags(TAG_A), EventCriteria.havingTags(TAG_B));
        EventCriteria equal = EventCriteria.either(EventCriteria.havingTags(TAG_A), EventCriteria.havingTags(TAG_B));

        EventCriteria result = first.or(equal);

        assertEquals(first, result);
        assertEquals(2, result.flatten().size());
    }

    @Test
    void anOrCombinedWithItselfIsThatOr() {
        EventCriteria testSubject =
                EventCriteria.either(EventCriteria.havingTags(TAG_A), EventCriteria.havingTags(TAG_B));

        assertEquals(testSubject, testSubject.or(testSubject));
    }

    @Test
    void theOrBuilderChainsCriteria() {
        EventCriteria testSubject = EventCriteria.havingTags(TAG_A).andBeingOneOfTypes(ONE)
                                                 .or().havingTags(TAG_B).andBeingOneOfTypes(TWO);

        assertTrue(testSubject.matches(ONE, Set.of(TAG_A)));
        assertTrue(testSubject.matches(TWO, Set.of(TAG_B)));
        assertFalse(testSubject.matches(TWO, Set.of(TAG_A)));
        assertFalse(testSubject.matches(ONE, Set.of(TAG_B)));
        assertEquals(2, testSubject.flatten().size());
    }

    @Test
    void theOrBuilderOffersTheSameTagVariantsAsTheFactoryMethods() {
        EventCriteria base = EventCriteria.havingTags(TAG_A);

        assertTrue(base.or().havingTags(Set.of(TAG_B)).matches(ONE, Set.of(TAG_B)));
        assertTrue(base.or().havingTags(TAG_B).matches(ONE, Set.of(TAG_B)));
        assertTrue(base.or().havingTags("b", "2").matches(ONE, Set.of(TAG_B)));
    }

    @Test
    void theOrBuilderCanBeFollowedByAnyTagAndTypeRestrictions() {
        EventCriteria testSubject = EventCriteria.havingTags(TAG_A).or().havingAnyTag().andBeingOneOfTypes(TWO);

        assertTrue(testSubject.matches(ONE, Set.of(TAG_A)));
        assertTrue(testSubject.matches(TWO, Set.of()));
        assertFalse(testSubject.matches(ONE, Set.of()));
    }

    @Test
    void theOrBuilderWithAnyTagMatchesEverything() {
        EventCriteria testSubject = EventCriteria.havingTags(TAG_A).or().havingAnyTag();

        assertTrue(testSubject.matches(ONE, Set.of()));
        assertTrue(testSubject.matches(THREE, Set.of(TAG_B)));
    }

    @Test
    void theOrBuilderRejectsNullCriteria() {
        assertEquals("orCriteria may not be null", assertThrows(NullPointerException.class,
                () -> new OrEventCriteriaBuilder(null)).getMessage());
    }

    // --- either ---

    @Test
    void eitherMatchesWhenAnyOfTheGivenCriteriaMatches() {
        EventCriteria testSubject = EventCriteria.either(EventCriteria.havingTags(TAG_A).andBeingOneOfTypes(ONE),
                                                         EventCriteria.havingTags(TAG_B).andBeingOneOfTypes(TWO));

        assertTrue(testSubject.matches(ONE, Set.of(TAG_A)));
        assertTrue(testSubject.matches(TWO, Set.of(TAG_B)));
        assertFalse(testSubject.matches(THREE, Set.of(TAG_A, TAG_B)));
    }

    @Test
    void eitherAcceptsACollectionAndRemovesDuplicates() {
        EventCriteria testSubject = EventCriteria.either(
                List.of(EventCriteria.havingTags(TAG_A), EventCriteria.havingTags(TAG_A)));

        assertEquals(1, testSubject.flatten().size());
    }

    @Test
    void eitherRejectsANullCollection() {
        assertEquals("The eventCriteria cannot be null.", assertThrows(NullPointerException.class,
                () -> EventCriteria.either((java.util.Collection<EventCriteria>) null)).getMessage());
    }

    @Test
    void anEmptyEitherMatchesNothing() {
        EventCriteria testSubject = EventCriteria.either(List.of());

        assertFalse(testSubject.matches(ONE, Set.of(TAG_A)));
        assertFalse(testSubject.hasCriteria());
    }

    @Test
    void eitherHasCriteriaWhenAnyMemberHasOne() {
        assertTrue(EventCriteria.either(EventCriteria.havingTags(TAG_A), EventCriteria.havingAnyTag())
                                .hasCriteria());
        assertFalse(EventCriteria.either(EventCriteria.havingAnyTag(), EventCriteria.havingAnyTag()).hasCriteria());
    }

    @Test
    void eitherHasValueEqualityAndAReadableDescription() {
        EventCriteria first = EventCriteria.either(EventCriteria.havingTags(TAG_A), EventCriteria.havingTags(TAG_B));
        EventCriteria equal = EventCriteria.either(EventCriteria.havingTags(TAG_B), EventCriteria.havingTags(TAG_A));
        EventCriteria different = EventCriteria.either(EventCriteria.havingTags(TAG_A));

        assertEquals(first, equal);
        assertEquals(first.hashCode(), equal.hashCode());
        assertNotEquals(first, different);
        assertTrue(first.toString().startsWith("OrEventCriteria[criteria="));
    }

    @Test
    void eitherOfNestedOrsFlattensThemIntoOne() {
        EventCriteria nested = EventCriteria.either(
                EventCriteria.either(EventCriteria.havingTags(TAG_A), EventCriteria.havingTags(TAG_B)),
                EventCriteria.havingTags(Tag.of("c", "3")));

        assertEquals(3, nested.flatten().size());
    }

    // --- mapEachCriterion ---

    @Test
    void mapEachCriterionReturnsTheSameCriteriaWhenNothingChanges() {
        EventCriteria testSubject = EventCriteria.havingTags(TAG_A).or().havingTags(TAG_B);

        assertSame(testSubject, testSubject.mapEachCriterion(criterion -> criterion));
    }

    @Test
    void mapEachCriterionRebuildsTheCriteriaFromTheMappedCriterion() {
        EventCriteria testSubject = EventCriteria.havingTags(TAG_A);

        EventCriteria result = testSubject.mapEachCriterion(
                criterion -> EventCriteria.havingTags(TAG_B));

        assertNotEquals(testSubject, result);
        assertTrue(result.matches(ONE, Set.of(TAG_B)));
        assertFalse(result.matches(ONE, Set.of(TAG_A)));
    }

    @Test
    void mapEachCriterionAppliesTheMapperToEveryCriterion() {
        EventCriteria testSubject = EventCriteria.havingTags(TAG_A).andBeingOneOfTypes(ONE)
                                                 .or().havingTags(TAG_B).andBeingOneOfTypes(TWO);

        EventCriteria result = testSubject.mapEachCriterion(
                criterion -> EventCriteria.havingTags(criterion.tags()).andBeingOneOfTypes(THREE));

        assertEquals(2, result.flatten().size());
        assertTrue(result.matches(THREE, Set.of(TAG_A)));
        assertTrue(result.matches(THREE, Set.of(TAG_B)));
        assertFalse(result.matches(ONE, Set.of(TAG_A)));
    }

    @Test
    void mapEachCriterionLeavesMatchAnythingAlone() {
        EventCriteria anyEvent = EventCriteria.havingAnyTag();

        assertSame(anyEvent, anyEvent.mapEachCriterion(criterion -> {
            throw new AssertionError("There is no criterion to map");
        }));
    }

    @Test
    void mapEachCriterionRejectsANullMapperAndNullResults() {
        EventCriteria testSubject = EventCriteria.havingTags(TAG_A);

        assertEquals("The mapper cannot be null.", assertThrows(NullPointerException.class,
                () -> testSubject.mapEachCriterion(null)).getMessage());
        assertEquals("The mapper must not return a null EventCriteria.", assertThrows(NullPointerException.class,
                () -> testSubject.mapEachCriterion(criterion -> null)).getMessage());
    }

    // --- sealed hierarchy ---

    @Test
    void tagAndTypeCriteriaAreCriteriaThemselves() {
        EventCriteria testSubject = EventCriteria.havingTags(TAG_A).andBeingOneOfTypes(ONE);

        assertInstanceOf(EventCriterion.class, testSubject);
        assertSame(testSubject, testSubject.flatten().iterator().next());
    }
}
