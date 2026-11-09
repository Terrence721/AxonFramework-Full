package org.axonframework.common.caching;

import org.axonframework.common.Registration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeakReferenceCacheTest {

    @Test
    void registerAndNotifyListener() {
        WeakReferenceCache cache = new WeakReferenceCache();
        List<String> events = new ArrayList<>();
        Registration registration = cache.registerCacheEntryListener(new Cache.EntryListener() {
            @Override
            public void onEntryCreated(Object key, Object value) {
                events.add("created-" + key);
            }

            @Override
            public void onEntryUpdated(Object key, Object value) {
                events.add("updated-" + key);
            }

            @Override
            public void onEntryRemoved(Object key) {
                events.add("removed-" + key);
            }

            @Override
            public void onEntryRead(Object key, Object value) {
                events.add("read-" + key);
            }

            @Override
            public Object clone() {
                return this;
            }

            @Override
            public void onEntryExpired(Object key) {
                events.add("expired-" + key);
            }
        });

        cache.put("key", "value");
        cache.put("key", "value2");
        assertEquals("value2", cache.get("key"));
        assertTrue(cache.containsKey("key"));
        assertTrue(cache.remove("key"));
        assertFalse(cache.remove("key"));
        registration.cancel();

        assertTrue(events.contains("created-key"));
        assertTrue(events.contains("updated-key"));
        assertTrue(events.contains("read-key"));
        assertTrue(events.contains("removed-key"));
    }

    @Test
    void putNullThrows() {
        WeakReferenceCache cache = new WeakReferenceCache();

        assertThrows(IllegalArgumentException.class, () -> cache.put("key", null));
        assertThrows(IllegalArgumentException.class, () -> cache.putIfAbsent("key", null));
    }

    @Test
    void putIfAbsentAndComputeIfAbsent() {
        WeakReferenceCache cache = new WeakReferenceCache();

        assertTrue(cache.putIfAbsent("key", "value"));
        assertFalse(cache.putIfAbsent("key", "value2"));
        assertEquals("value", cache.get("key"));

        AtomicInteger calls = new AtomicInteger();
        assertEquals("value", cache.computeIfAbsent("key", () -> {
            calls.incrementAndGet();
            return "new";
        }));
        assertEquals(0, calls.get());

        assertEquals("new2", cache.computeIfAbsent("key2", () -> "new2"));
        assertThrows(IllegalArgumentException.class, () -> cache.computeIfAbsent("key3", () -> null));
    }

    @Test
    void removeAll() {
        WeakReferenceCache cache = new WeakReferenceCache();
        cache.put("a", "1");
        cache.put("b", "2");

        cache.removeAll();

        assertNull(cache.get("a"));
        assertNull(cache.get("b"));
    }

    @Test
    void computeIfPresentUpdatesAndRemoves() {
        WeakReferenceCache cache = new WeakReferenceCache();
        cache.put("key", "value");

        cache.computeIfPresent("key", value -> "updated");
        assertEquals("updated", cache.get("key"));

        cache.computeIfPresent("key", value -> null);
        assertNull(cache.get("key"));
    }

    @Test
    void getNullKeyThrows() {
        WeakReferenceCache cache = new WeakReferenceCache();

        assertThrows(IllegalArgumentException.class, () -> cache.get(null));
        assertThrows(IllegalArgumentException.class, () -> cache.containsKey(null));
    }
}