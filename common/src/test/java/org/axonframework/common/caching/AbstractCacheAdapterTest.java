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
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link AbstractCacheAdapter}, through a minimal map backed implementation.
 */
class AbstractCacheAdapterTest {

    private final FakeAdapter testSubject = new FakeAdapter();

    @Test
    void computeIfPresentReplacesTheValueWithTheUpdateResult() {
        testSubject.put("key", "value");

        testSubject.<String>computeIfPresent("key", current -> current + "!");

        assertEquals("value!", testSubject.get("key"));
    }

    @Test
    void computeIfPresentDoesNothingWhenKeyIsAbsent() {
        List<String> invocations = new ArrayList<>();

        testSubject.<String>computeIfPresent("missing", current -> {
            invocations.add(current);
            return current;
        });

        assertTrue(invocations.isEmpty());
        assertFalse(testSubject.containsKey("missing"));
    }

    @Test
    void computeIfPresentRemovesTheEntryWhenUpdateReturnsNull() {
        testSubject.put("key", "value");

        testSubject.<String>computeIfPresent("key", current -> null);

        assertFalse(testSubject.containsKey("key"));
    }

    @Test
    void computeIfPresentRetriesWithTheLatestValueWhenReplaceLosesARace() {
        testSubject.put("key", "first");
        testSubject.beforeNextReplace = () -> testSubject.put("key", "concurrent");
        List<String> seen = new ArrayList<>();

        testSubject.<String>computeIfPresent("key", current -> {
            seen.add(current);
            return current + "+updated";
        });

        assertEquals(List.of("first", "concurrent"), seen);
        assertEquals("concurrent+updated", testSubject.get("key"));
    }

    @Test
    void registeringListenerRegistersAdapterOnceWithUnderlyingCache() {
        testSubject.registerCacheEntryListener(new Cache.EntryListenerAdapter());

        assertEquals(List.of("adapter-1"), testSubject.registered);
        assertTrue(testSubject.cancelled.isEmpty());
    }

    @Test
    void cancellingRegistrationDeregistersAdapterFromUnderlyingCache() {
        Registration registration = testSubject.registerCacheEntryListener(new Cache.EntryListenerAdapter());

        assertTrue(registration.cancel());

        assertEquals(List.of("adapter-1"), testSubject.cancelled);
    }

    @Test
    void cancellingRegistrationTwiceOnlyDeregistersOnce() {
        Registration registration = testSubject.registerCacheEntryListener(new Cache.EntryListenerAdapter());

        assertTrue(registration.cancel());
        assertFalse(registration.cancel());

        assertEquals(1, testSubject.cancelled.size());
    }

    @Test
    void differentListenersAreRegisteredAndCancelledIndependently() {
        Registration first = testSubject.registerCacheEntryListener(new Cache.EntryListenerAdapter());
        Registration second = testSubject.registerCacheEntryListener(new Cache.EntryListenerAdapter());

        assertEquals(List.of("adapter-1", "adapter-2"), testSubject.registered);

        assertTrue(first.cancel());
        assertEquals(List.of("adapter-1"), testSubject.cancelled);
        assertTrue(second.cancel());
        assertEquals(List.of("adapter-1", "adapter-2"), testSubject.cancelled);
    }

    @Test
    void listenerCanBeRegisteredAgainAfterItWasCancelled() {
        Cache.EntryListener listener = new Cache.EntryListenerAdapter();

        testSubject.registerCacheEntryListener(listener).cancel();
        testSubject.registerCacheEntryListener(listener);

        assertEquals(List.of("adapter-1", "adapter-2"), testSubject.registered);
    }

    private static final class FakeAdapter extends AbstractCacheAdapter<String> {

        private final Map<Object, Object> store = new ConcurrentHashMap<>();
        private final List<String> registered = new ArrayList<>();
        private final List<String> cancelled = new ArrayList<>();
        private @Nullable Runnable beforeNextReplace;

        @SuppressWarnings("unchecked")
        @Override
        public @Nullable <K, V> V get(K key) {
            return (V) store.get(key);
        }

        @Override
        public void put(Object key, Object value) {
            store.put(key, value);
        }

        @Override
        public boolean putIfAbsent(Object key, Object value) {
            return store.putIfAbsent(key, value) == null;
        }

        @Override
        public boolean remove(Object key) {
            return store.remove(key) != null;
        }

        @Override
        public void removeAll() {
            store.clear();
        }

        @Override
        public boolean containsKey(Object key) {
            return store.containsKey(key);
        }

        @Override
        protected <V> boolean replace(Object key, V oldValue, V newValue) {
            if (beforeNextReplace != null) {
                Runnable interference = beforeNextReplace;
                beforeNextReplace = null;
                interference.run();
            }
            return store.replace(key, oldValue, newValue);
        }

        @Override
        protected String createListenerAdapter(EntryListener cacheEntryListener) {
            return "adapter-" + (registered.size() + 1);
        }

        @Override
        protected Registration doRegisterListener(String listenerAdapter) {
            registered.add(listenerAdapter);
            return () -> {
                cancelled.add(listenerAdapter);
                return true;
            };
        }
    }
}
