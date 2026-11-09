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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssertTest {

    @Test
    void statePassesWhenTrue() {
        Assert.state(true, () -> "msg");
    }

    @Test
    void stateThrowsWhenFalse() {
        assertThrows(IllegalStateException.class, () -> Assert.state(false, () -> "state message"));
    }

    @Test
    void isTruePassesWhenTrue() {
        Assert.isTrue(true, () -> "msg");
    }

    @Test
    void isTrueThrowsWhenFalse() {
        assertThrows(IllegalArgumentException.class, () -> Assert.isTrue(false, () -> "true message"));
    }

    @Test
    void isFalsePassesWhenFalse() {
        Assert.isFalse(false, () -> "msg");
    }

    @Test
    void isFalseThrowsWhenTrue() {
        assertThrows(IllegalArgumentException.class, () -> Assert.isFalse(true, () -> "false message"));
    }

    @Test
    void notNullReturnsNoExceptionWhenValuePresent() {
        Assert.notNull("value", () -> "should not be null");
    }

    @Test
    void notNullThrowsWhenValueIsNull() {
        assertThrows(IllegalArgumentException.class, () -> Assert.notNull(null, () -> "message"));
    }

    @Test
    void nonNullReturnsProvidedValue() {
        assertEquals("value", Assert.nonNull("value", () -> "message"));
    }

    @Test
    void nonNullThrowsWhenValueIsNull() {
        assertThrows(IllegalArgumentException.class, () -> Assert.nonNull(null, () -> "message"));
    }

    @Test
    void assertThatPassesAndThrows() {
        Assert.assertThat(5, v -> v > 0, () -> new IllegalArgumentException("bad"));

        assertThrows(IllegalArgumentException.class, () ->
                Assert.assertThat(0, v -> v > 0, () -> new IllegalArgumentException("bad")));
    }

    @Test
    void assertNonNullPassesAndThrows() {
        Assert.assertNonNull("value", () -> new IllegalArgumentException("null"));

        assertThrows(IllegalArgumentException.class, () ->
                Assert.assertNonNull(null, () -> new IllegalArgumentException("null")));
    }

    @Test
    void nonEmptyReturnsValueAndThrows() {
        assertEquals("value", Assert.nonEmpty("value", "msg"));
        assertThrows(IllegalArgumentException.class, () -> Assert.nonEmpty("", "msg"));
        assertThrows(IllegalArgumentException.class, () -> Assert.nonEmpty(null, "msg"));
    }

    @Test
    void assertStrictPositiveIntPassesAndThrows() {
        Assert.assertStrictPositive(1, "msg");

        assertThrows(IllegalArgumentException.class, () -> Assert.assertStrictPositive(0, "msg"));
        assertThrows(IllegalArgumentException.class, () -> Assert.assertStrictPositive(-1, "msg"));
    }

    @Test
    void assertStrictPositiveLongPassesAndThrows() {
        Assert.assertStrictPositive(1L, "msg");

        assertThrows(IllegalArgumentException.class, () -> Assert.assertStrictPositive(0L, "msg"));
        assertThrows(IllegalArgumentException.class, () -> Assert.assertStrictPositive(-1L, "msg"));
    }
}