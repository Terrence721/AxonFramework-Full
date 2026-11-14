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
import org.junit.jupiter.api.Test;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test class validating the default methods and the {@link Cache.EntryListenerAdapter} of the
 * {@link Cache} interface, through a minimal map-backed implementation.
 */
class CacheTest {

    private final Cache testSubject = new MapBackedCache();

    @Test
    void computeIfAbsentReturnsExistingValueWhenPresent() {
        testSubject.put("key", "value");

        Object result = testSubject.computeIfAbsent("key", () -> "supplied");

        assertEquals("value", result);
    }

    @Test
    void computeIfAbsentPutsAndReturnsSuppliedValueWhenAbsent() {
        Object result = testSubject.computeIfAbsent("key", () -> "supplied");

        assertEquals("supplied", result);
        assertEquals("supplied", testSubject.get("key"));
    }

    @Test
    void computeIfAbsentThrowsWhenSupplierReturnsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> testSubject.computeIfAbsent("key", () -> null));
    }

    @Test
    void removeAllIsUnsupportedByDefault() {
        assertThrows(UnsupportedOperationException.class, testSubject::removeAll);
    }

    @Test
    void computeIfPresentIsUnsupportedByDefault() {
        assertThrows(UnsupportedOperationException.class,
                () -> testSubject.computeIfPresent("key", current -> current));
    }

    @Test
    void entryListenerAdapterInvokesNoOpCallbacks() {
        Cache.EntryListenerAdapter adapter = new Cache.EntryListenerAdapter();

        adapter.onEntryExpired("expired");
        adapter.onEntryRemoved("removed");
        adapter.onEntryUpdated("updated", "value");
        adapter.onEntryCreated("created", "value");
        adapter.onEntryRead("read", "value");
    }

    @Test
    void entryListenerAdapterCloneReturnsCopyWhenCloneable() {
        CloneableAdapter adapter = new CloneableAdapter();

        Object copy;
        try {
            copy = adapter.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError("Clone should succeed for Cloneable implementation", e);
        }

        assertNotSame(adapter, copy);
        assertEquals(CloneableAdapter.class, copy.getClass());
    }

    private static final class MapBackedCache implements Cache {

        private final ConcurrentMap<Object, Object> store = new ConcurrentHashMap<>();

        @Override
        @SuppressWarnings("unchecked")
        public <K, V> V get(K key) {
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
        public boolean containsKey(Object key) {
            return store.containsKey(key);
        }

        @Override
        public Registration registerCacheEntryListener(EntryListener cacheEntryListener) {
            return () -> true;
        }
    }

    private static final class CloneableAdapter extends Cache.EntryListenerAdapter
            implements Cloneable {
        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }
}