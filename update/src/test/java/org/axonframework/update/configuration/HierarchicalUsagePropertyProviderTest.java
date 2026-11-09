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

package org.axonframework.update.configuration;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link HierarchicalUsagePropertyProvider} and the {@link DefaultUsagePropertyProvider}.
 */
class HierarchicalUsagePropertyProviderTest {

    private static UsagePropertyProvider provider(int priority, @Nullable Boolean disabled, @Nullable String url) {
        return new UsagePropertyProvider() {
            @Override
            public @Nullable Boolean getDisabled() {
                return disabled;
            }

            @Override
            public @Nullable String getUrl() {
                return url;
            }

            @Override
            public int priority() {
                return priority;
            }
        };
    }

    @Test
    void constructorRejectsNullProviders() {
        NullPointerException exception =
                assertThrows(NullPointerException.class, () -> new HierarchicalUsagePropertyProvider(null));

        assertEquals("The providers may not be null.", exception.getMessage());
    }

    @Test
    void highestPriorityProviderWins() {
        HierarchicalUsagePropertyProvider testSubject = new HierarchicalUsagePropertyProvider(List.of(
                provider(1, false, "low"),
                provider(100, true, "high"),
                provider(50, false, "mid")
        ));

        assertTrue(testSubject.getDisabled());
        assertEquals("high", testSubject.getUrl());
    }

    @Test
    void eachPropertyFallsThroughIndependentlyWhenNull() {
        HierarchicalUsagePropertyProvider testSubject = new HierarchicalUsagePropertyProvider(List.of(
                provider(100, true, null),
                provider(50, null, "from-mid"),
                provider(1, false, "from-low")
        ));

        assertTrue(testSubject.getDisabled());
        assertEquals("from-mid", testSubject.getUrl());
    }

    @Test
    void usesDefaultsWhenNoProviderHasAValue() {
        HierarchicalUsagePropertyProvider testSubject =
                new HierarchicalUsagePropertyProvider(List.of(provider(1, null, null)));

        assertFalse(testSubject.getDisabled());
        assertEquals("", testSubject.getUrl());
    }

    @Test
    void usesDefaultsWithoutAnyProviders() {
        HierarchicalUsagePropertyProvider testSubject = new HierarchicalUsagePropertyProvider(List.of());

        assertFalse(testSubject.getDisabled());
        assertEquals("", testSubject.getUrl());
    }

    @Test
    void providersWithEqualPriorityKeepTheirGivenOrder() {
        HierarchicalUsagePropertyProvider testSubject = new HierarchicalUsagePropertyProvider(List.of(
                provider(5, null, "first"),
                provider(5, null, "second")
        ));

        assertEquals("first", testSubject.getUrl());
    }

    @Test
    void prioritySortingDoesNotReorderTheCallersList() {
        UsagePropertyProvider low = provider(1, null, "low");
        UsagePropertyProvider high = provider(9, null, "high");
        List<UsagePropertyProvider> given = new java.util.ArrayList<>(List.of(low, high));

        new HierarchicalUsagePropertyProvider(given);

        assertSame(low, given.get(0));
        assertSame(high, given.get(1));
    }

    @Test
    void combinedProviderReportsNeutralPriority() {
        assertEquals(0, new HierarchicalUsagePropertyProvider(List.of()).priority());
    }

    @Test
    void defaultProviderIsEnabledPointsAtTheAxoniqEndpointAndHasLowestPriority() {
        DefaultUsagePropertyProvider testSubject = DefaultUsagePropertyProvider.INSTANCE;

        assertFalse(testSubject.getDisabled());
        assertEquals("https://get.axoniq.io/updates/framework", testSubject.getUrl());
        assertEquals(Integer.MIN_VALUE, testSubject.priority());
    }

    @Test
    void defaultProviderIsOverriddenByAnyOtherProvider() {
        HierarchicalUsagePropertyProvider testSubject = new HierarchicalUsagePropertyProvider(List.of(
                DefaultUsagePropertyProvider.INSTANCE,
                provider(Integer.MIN_VALUE + 1, true, "custom")
        ));

        assertTrue(testSubject.getDisabled());
        assertEquals("custom", testSubject.getUrl());
    }
}
