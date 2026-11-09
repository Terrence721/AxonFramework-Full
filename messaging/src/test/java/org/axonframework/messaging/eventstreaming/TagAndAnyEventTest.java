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

import org.axonframework.messaging.core.Context;
import org.axonframework.messaging.core.QualifiedName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link Tag} and the match-anything {@code AnyEvent} criteria.
 */
class TagAndAnyEventTest {

    // --- Tag ---

    @Test
    void tagExposesKeyAndValueAndHasValueEquality() {
        Tag tag = new Tag("key", "value");

        assertEquals("key", tag.key());
        assertEquals("value", tag.value());
        assertEquals(Tag.of("key", "value"), tag);
        assertEquals(Tag.of("key", "value").hashCode(), tag.hashCode());
        assertNotEquals(Tag.of("key", "other"), tag);
    }

    @Test
    void theConstructorRejectsANullKeyOrValueWithAnIllegalArgumentException() {
        assertEquals("A Tag's key cannot be null", assertThrows(IllegalArgumentException.class,
                () -> new Tag(null, "value")).getMessage());
        assertEquals("A Tag's value cannot be null", assertThrows(IllegalArgumentException.class,
                () -> new Tag("key", null)).getMessage());
    }

    @Test
    void theFactoryMethodRejectsANullKeyOrValueWithANullPointerException() {
        assertEquals("The key of a Tag is required.", assertThrows(NullPointerException.class,
                () -> Tag.of(null, "value")).getMessage());
        assertEquals("The value of a Tag is required.", assertThrows(NullPointerException.class,
                () -> Tag.of("key", null)).getMessage());
    }

    @Test
    void tagsCanBeStoredInAndReadFromAContext() {
        Set<Tag> tags = Set.of(Tag.of("a", "1"), Tag.of("b", "2"));

        Context context = Tag.addToContext(Context.empty(), tags);

        assertEquals(tags, Tag.fromContext(context).orElseThrow());
    }

    @Test
    void aContextWithoutTagsHasNone() {
        assertTrue(Tag.fromContext(Context.empty()).isEmpty());
    }

    // --- AnyEvent ---

    @Test
    void anyEventMatchesEverythingAndHasNoCriteria() {
        EventCriteria anyEvent = EventCriteria.havingAnyTag();

        assertTrue(anyEvent.matches(new QualifiedName("any.type"), Set.of()));
        assertTrue(anyEvent.matches(new QualifiedName("other.type"), Set.of(Tag.of("k", "v"))));
        assertFalse(anyEvent.hasCriteria());
        assertTrue(anyEvent.flatten().isEmpty());
    }

    @Test
    void anyEventIsASingleton() {
        assertSame(EventCriteria.havingAnyTag(), EventCriteria.havingAnyTag());
        assertEquals(EventCriteria.havingAnyTag(), EventCriteria.havingAnyTag().andBeingOfAnyType());
        assertEquals("AnyEvent", EventCriteria.havingAnyTag().toString());
    }

    @Test
    void combiningAnyEventWithAnythingStillMatchesEverything() {
        EventCriteria anyEvent = EventCriteria.havingAnyTag();

        assertSame(anyEvent, anyEvent.or(EventCriteria.havingTags("k", "v")));
    }

    @Test
    void restrictingAnyEventToTypesOnlyFiltersOnTheTypes() {
        EventCriteria result = EventCriteria.havingAnyTag().andBeingOneOfTypes(new QualifiedName("a.Type"));

        assertTrue(result.matches(new QualifiedName("a.Type"), Set.of()));
        assertTrue(result.matches(new QualifiedName("a.Type"), Set.of(Tag.of("k", "v"))));
        assertFalse(result.matches(new QualifiedName("b.Type"), Set.of()));
    }

    @Test
    void restrictingAnyEventToNullTypesIsRejected() {
        assertEquals("The provided types should not be null", assertThrows(NullPointerException.class,
                () -> EventCriteria.havingAnyTag().andBeingOneOfTypes((Set<QualifiedName>) null)).getMessage());
    }
}
