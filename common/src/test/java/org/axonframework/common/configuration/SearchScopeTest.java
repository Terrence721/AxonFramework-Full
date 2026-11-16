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

package org.axonframework.common.configuration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for the {@link SearchScope} enum to achieve 100% coverage.
 */
class SearchScopeTest {

    @Test
    void allEnumValuesArePresent() {
        SearchScope[] values = SearchScope.values();
        assertNotNull(values);
        assertEquals(3, values.length);
    }

    @Test
    void allHasCorrectOrdinal() {
        assertEquals(0, SearchScope.ALL.ordinal());
    }

    @Test
    void currentHasCorrectOrdinal() {
        assertEquals(1, SearchScope.CURRENT.ordinal());
    }

    @Test
    void ancestorsHasCorrectOrdinal() {
        assertEquals(2, SearchScope.ANCESTORS.ordinal());
    }

    @Test
    void valueOfAllReturnsCorrectEnum() {
        assertSame(SearchScope.ALL, SearchScope.valueOf("ALL"));
    }

    @Test
    void valueOfCurrentReturnsCorrectEnum() {
        assertSame(SearchScope.CURRENT, SearchScope.valueOf("CURRENT"));
    }

    @Test
    void valueOfAncestorsReturnsCorrectEnum() {
        assertSame(SearchScope.ANCESTORS, SearchScope.valueOf("ANCESTORS"));
    }

    @Test
    void enumToStringReturnsName() {
        assertEquals("ALL", SearchScope.ALL.toString());
        assertEquals("CURRENT", SearchScope.CURRENT.toString());
        assertEquals("ANCESTORS", SearchScope.ANCESTORS.toString());
    }

    @Test
    void enumCanBeComparedWithEquals() {
        assertSame(SearchScope.ALL, SearchScope.ALL);
        assertSame(SearchScope.CURRENT, SearchScope.CURRENT);
        assertSame(SearchScope.ANCESTORS, SearchScope.ANCESTORS);
    }

    @Test
    void enumCanBeUsedInSwitch() {
        String result = switch (SearchScope.ALL) {
            case ALL -> "all";
            case CURRENT -> "current";
            case ANCESTORS -> "ancestors";
        };
        assertEquals("all", result);

        result = switch (SearchScope.CURRENT) {
            case ALL -> "all";
            case CURRENT -> "current";
            case ANCESTORS -> "ancestors";
        };
        assertEquals("current", result);

        result = switch (SearchScope.ANCESTORS) {
            case ALL -> "all";
            case CURRENT -> "current";
            case ANCESTORS -> "ancestors";
        };
        assertEquals("ancestors", result);
    }

    @Test
    void allEnumValuesAreInOrder() {
        SearchScope[] values = SearchScope.values();
        assertTrue(values[0] == SearchScope.ALL);
        assertTrue(values[1] == SearchScope.CURRENT);
        assertTrue(values[2] == SearchScope.ANCESTORS);
    }
}