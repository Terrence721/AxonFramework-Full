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

package org.axonframework.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BuilderUtilsTest {

    @Test
    void assertNonBlankReturnsValueWhenNotBlank() {
        assertEquals("value", BuilderUtils.assertNonBlank("value", "blank"));
    }

    @Test
    void assertNonBlankThrowsForBlankString() {
        assertThrows(AxonConfigurationException.class,
                () -> BuilderUtils.assertNonBlank("   ", "message"));
    }

    @Test
    void assertNonBlankThrowsForNull() {
        assertThrows(AxonConfigurationException.class, () -> BuilderUtils.assertNonBlank(null, "x"));
    }

    @Test
    void assertNonEmptyThrowsForEmptyString() {
        assertThrows(AxonConfigurationException.class,
                () -> BuilderUtils.assertNonEmpty("", "message"));
    }

    @Test
    void assertNonEmptyReturnsNoExceptionWhenPopulated() {
        BuilderUtils.assertNonEmpty("value", "x");
        assertThrows(AxonConfigurationException.class, () -> BuilderUtils.assertNonEmpty(null, "x"));
    }

    @Test
    void assertStrictPositiveAllowsPositiveValue() {
        BuilderUtils.assertStrictPositive(1, "x");
    }

    @Test
    void assertStrictPositiveThrowsForZero() {
        assertThrows(AxonConfigurationException.class, () -> BuilderUtils.assertStrictPositive(0, "x"));
    }

    @Test
    void assertStrictPositiveAllowsPositiveLongAndThrowsForZero() {
        BuilderUtils.assertStrictPositive(1L, "x");
        assertThrows(AxonConfigurationException.class, () -> BuilderUtils.assertStrictPositive(0L, "x"));
    }

    @Test
    void assertPositiveAllowsZeroAndThrowsForNegative() {
        BuilderUtils.assertPositive(0, "x");
        BuilderUtils.assertPositive(1L, "x");
        assertThrows(AxonConfigurationException.class, () -> BuilderUtils.assertPositive(-1, "x"));
        assertThrows(AxonConfigurationException.class, () -> BuilderUtils.assertPositive(-1L, "x"));
    }

    @Test
    void assertThatPassesAndThrows() {
        BuilderUtils.assertThat(1, value -> value > 0, "x");
        assertThrows(AxonConfigurationException.class,
                () -> BuilderUtils.assertThat(0, value -> value > 0, "x"));
    }

    @Test
    void assertThatWithNegativeValues() {
        assertThrows(AxonConfigurationException.class,
                () -> BuilderUtils.assertThat(-5, value -> value > 0, "negative"));
        assertThrows(AxonConfigurationException.class,
                () -> BuilderUtils.assertThat(-10, value -> value > 0, "negative"));
    }

    @Test
    void assertNonNullReturnsValueAndThrowsWhenNull() {
        BuilderUtils.assertNonNull("value", "x");
        assertThrows(AxonConfigurationException.class, () -> BuilderUtils.assertNonNull(null, "x"));
    }

    @Test
    void assertNonNullWithDifferentMessages() {
        assertThrows(AxonConfigurationException.class, () -> BuilderUtils.assertNonNull(null, "runtime"));
        assertThrows(AxonConfigurationException.class, () -> BuilderUtils.assertNonNull(null, "state"));
    }

    @Test
    void assertNonEmptyWithVariousMessages() {
        assertThrows(AxonConfigurationException.class, () -> BuilderUtils.assertNonEmpty("", "msg1"));
        assertThrows(AxonConfigurationException.class, () -> BuilderUtils.assertNonEmpty(null, "msg2"));
        BuilderUtils.assertNonEmpty("test", "msg3");
    }

    @Test
    void assertNonBlankWithVariousMessages() {
        assertThrows(AxonConfigurationException.class, () -> BuilderUtils.assertNonBlank("", "msg1"));
        assertThrows(AxonConfigurationException.class, () -> BuilderUtils.assertNonBlank(null, "msg2"));
        assertEquals("test", BuilderUtils.assertNonBlank("test", "msg3"));
    }

    @Test
    void assertPositiveLongBoundaryValues() {
        BuilderUtils.assertPositive(0, "x");
        BuilderUtils.assertPositive(1L, "x");
        assertThrows(AxonConfigurationException.class, () -> BuilderUtils.assertPositive(-1L, "x"));
    }

    @Test
    void assertPositiveIntBoundaryValues() {
        BuilderUtils.assertPositive(0, "x");
        BuilderUtils.assertPositive(1, "x");
        assertThrows(AxonConfigurationException.class, () -> BuilderUtils.assertPositive(-1, "x"));
    }

    @Test
    void assertStrictPositiveLongBoundaryValues() {
        BuilderUtils.assertStrictPositive(1L, "x");
        assertThrows(AxonConfigurationException.class, () -> BuilderUtils.assertStrictPositive(0L, "x"));
        assertThrows(AxonConfigurationException.class, () -> BuilderUtils.assertStrictPositive(Long.MIN_VALUE, "x"));
    }

    @Test
    void assertStrictPositiveIntBoundaryValues() {
        BuilderUtils.assertStrictPositive(1, "x");
        assertThrows(AxonConfigurationException.class, () -> BuilderUtils.assertStrictPositive(0, "x"));
        assertThrows(AxonConfigurationException.class, () -> BuilderUtils.assertStrictPositive(Integer.MIN_VALUE, "x"));
    }
}
