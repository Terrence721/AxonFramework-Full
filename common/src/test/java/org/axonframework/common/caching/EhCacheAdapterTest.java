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

package org.axonframework.common.caching;

import org.axonframework.common.Registration;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.core.Ehcache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link EhCacheAdapter} against a real, heap based Ehcache.
 */
class EhCacheAdapterTest {

    private CacheManager cacheManager;
    private EhCacheAdapter testSubject;

    @SuppressWarnings("rawtypes")
    @BeforeEach
    void setUp() {
        cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                                          .withCache("test", CacheConfigurationBuilder.newCacheConfigurationBuilder(
                                                  Object.class, Object.class, ResourcePoolsBuilder.heap(100)))
                                          .build(true);
        testSubject = new EhCacheAdapter((Ehcache) cacheManager.getCache("test", Object.class, Object.class));
    }

    @AfterEach
    void tearDown() {
        cacheManager.close();
    }

    @Test
    void putAndGetRoundTrip() {
        testSubject.put("key", "value");

        assertEquals("value", testSubject.get("key"));
        assertTrue(testSubject.containsKey("key"));
    }

    @Test
    void getReturnsNullForUnknownKey() {
        assertNull(testSubject.get("missing"));
        assertFalse(testSubject.containsKey("missing"));
    }

    @Test
    void putIfAbsentOnlyStoresWhenKeyIsAbsent() {
        assertTrue(testSubject.putIfAbsent("key", "first"));
        assertFalse(testSubject.putIfAbsent("key", "second"));

        assertEquals("first", testSubject.get("key"));
    }

    @Test
    void removeReportsWhetherAnEntryWasRemoved() {
        testSubject.put("key", "value");

        assertTrue(testSubject.remove("key"));
        assertFalse(testSubject.remove("key"));
        assertFalse(testSubject.containsKey("key"));
    }

    @Test
    void removeAllClearsTheCache() {
        testSubject.put("one", 1);
        testSubject.put("two", 2);

        testSubject.removeAll();

        assertFalse(testSubject.containsKey("one"));
        assertFalse(testSubject.containsKey("two"));
    }

    @Test
    void computeIfPresentUpdatesExistingValueOnly() {
        testSubject.put("key", 1);

        testSubject.<Integer>computeIfPresent("key", current -> current + 1);
        testSubject.<Integer>computeIfPresent("missing", current -> current + 1);

        assertEquals(2, (Integer) testSubject.get("key"));
        assertFalse(testSubject.containsKey("missing"));
    }

    @Test
    void computeIfPresentRemovesEntryWhenUpdateReturnsNull() {
        testSubject.put("key", 1);

        testSubject.<Integer>computeIfPresent("key", current -> null);

        assertFalse(testSubject.containsKey("key"));
    }

    @Test
    void listenerReceivesCreatedUpdatedAndRemovedEvents() {
        List<String> events = new ArrayList<>();
        testSubject.registerCacheEntryListener(recordingListener(events));

        testSubject.put("key", "one");
        testSubject.put("key", "two");
        testSubject.remove("key");

        assertEquals(List.of("created:key=one", "updated:key=two", "removed:key"), events);
    }

    @Test
    void cancelledRegistrationStopsEvents() {
        List<String> events = new ArrayList<>();
        Registration registration = testSubject.registerCacheEntryListener(recordingListener(events));

        assertTrue(registration.cancel());
        testSubject.put("key", "value");

        assertTrue(events.isEmpty());
    }

    private static Cache.EntryListener recordingListener(List<String> events) {
        return new Cache.EntryListenerAdapter() {
            @Override
            public void onEntryCreated(Object key, Object value) {
                events.add("created:" + key + "=" + value);
            }

            @Override
            public void onEntryUpdated(Object key, Object value) {
                events.add("updated:" + key + "=" + value);
            }

            @Override
            public void onEntryRemoved(Object key) {
                events.add("removed:" + key);
            }

            @Override
            public void onEntryExpired(Object key) {
                events.add("expired:" + key);
            }
        };
    }
}
