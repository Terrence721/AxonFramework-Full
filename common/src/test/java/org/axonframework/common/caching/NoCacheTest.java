package org.axonframework.common.caching;

import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoCacheTest {

    @Test
    void nothingIsCached() {
        NoCache cache = NoCache.INSTANCE;

        assertNull(cache.get("key"));
        assertFalse(cache.containsKey("key"));
        assertTrue(cache.putIfAbsent("key", "value"));
        cache.put("key", "value");
        assertFalse(cache.remove("key"));
        assertEquals("supplied", cache.computeIfAbsent("key", () -> "supplied"));
        cache.computeIfPresent("key", value -> "updated");
        cache.removeAll();
    }

    @Test
    void listenerRegistrationCancelsSuccessfully() {
        assertTrue(NoCache.INSTANCE.registerCacheEntryListener(new Cache.EntryListenerAdapter()).cancel());
    }

    @SuppressWarnings("unused")
    private static Supplier<String> unusedSupplier() {
        return () -> "unused";
    }
}