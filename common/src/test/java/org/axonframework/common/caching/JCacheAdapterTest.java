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

import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryExpiredListener;
import javax.cache.event.CacheEntryRemovedListener;
import javax.cache.event.CacheEntryUpdatedListener;
import javax.cache.event.EventType;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link JCacheAdapter}, using a map backed {@link javax.cache.Cache} stand-in since no JCache
 * implementation is on the classpath.
 */
@SuppressWarnings("unchecked")
class JCacheAdapterTest {

    private final Map<Object, Object> store = new ConcurrentHashMap<>();
    private final List<CacheEntryListenerConfiguration<Object, Object>> registeredConfigurations = new ArrayList<>();
    private final javax.cache.Cache<Object, Object> jCache = fakeJCache();
    private final JCacheAdapter testSubject = new JCacheAdapter(jCache);

    @SuppressWarnings("unchecked")
    private javax.cache.Cache<Object, Object> fakeJCache() {
        return (javax.cache.Cache<Object, Object>) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{javax.cache.Cache.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "get":
                            return store.get(args[0]);
                        case "put":
                            store.put(args[0], args[1]);
                            return null;
                        case "putIfAbsent":
                            return store.putIfAbsent(args[0], args[1]) == null;
                        case "remove":
                            return store.remove(args[0]) != null;
                        case "removeAll":
                            store.clear();
                            return null;
                        case "containsKey":
                            return store.containsKey(args[0]);
                        case "replace":
                            return store.replace(args[0], args[1], args[2]);
                        case "registerCacheEntryListener":
                            registeredConfigurations.add((CacheEntryListenerConfiguration<Object, Object>) args[0]);
                            return null;
                        case "deregisterCacheEntryListener":
                            registeredConfigurations.remove(args[0]);
                            return null;
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "equals":
                            return proxy == args[0];
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                }
        );
    }

    @Test
    void delegatesBasicOperationsToTheJCache() {
        testSubject.put("key", "value");

        assertEquals("value", testSubject.get("key"));
        assertTrue(testSubject.containsKey("key"));
        assertFalse(testSubject.putIfAbsent("key", "other"));
        assertTrue(testSubject.putIfAbsent("new", "fresh"));
        assertTrue(testSubject.remove("key"));
        assertFalse(testSubject.remove("key"));
        assertNull(testSubject.get("key"));
    }

    @Test
    void removeAllClearsTheJCache() {
        testSubject.put("one", 1);
        testSubject.put("two", 2);

        testSubject.removeAll();

        assertTrue(store.isEmpty());
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
    void registeringListenerRegistersSynchronousConfigurationWithoutOldValue() {
        testSubject.registerCacheEntryListener(new Cache.EntryListenerAdapter());

        assertEquals(1, registeredConfigurations.size());
        CacheEntryListenerConfiguration<Object, Object> configuration = registeredConfigurations.get(0);
        assertTrue(configuration.isSynchronous());
        assertFalse(configuration.isOldValueRequired());
        assertNull(configuration.getCacheEntryEventFilterFactory());
    }

    @Test
    void cancellingRegistrationDeregistersTheConfiguration() {
        Registration registration = testSubject.registerCacheEntryListener(new Cache.EntryListenerAdapter());

        assertTrue(registration.cancel());

        assertTrue(registeredConfigurations.isEmpty());
    }

    @Test
    void registeredConfigurationForwardsAllEventsToTheListener() {
        List<String> events = new ArrayList<>();
        testSubject.registerCacheEntryListener(new Cache.EntryListenerAdapter() {
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
        });
        Object listener = registeredConfigurations.get(0).getCacheEntryListenerFactory().create();

        ((CacheEntryCreatedListener<Object, Object>) listener).onCreated(List.of(event(EventType.CREATED, "a", 1)));
        ((CacheEntryUpdatedListener<Object, Object>) listener).onUpdated(List.of(event(EventType.UPDATED, "a", 2)));
        ((CacheEntryRemovedListener<Object, Object>) listener).onRemoved(List.of(event(EventType.REMOVED, "a", 2)));
        ((CacheEntryExpiredListener<Object, Object>) listener).onExpired(List.of(event(EventType.EXPIRED, "b", 3)));

        assertEquals(List.of("created:a=1", "updated:a=2", "removed:a", "expired:b"), events);
    }

    @Test
    void listenerFactoryAlwaysCreatesTheSameAdapterInstance() {
        testSubject.registerCacheEntryListener(new Cache.EntryListenerAdapter());
        CacheEntryListenerConfiguration<Object, Object> configuration = registeredConfigurations.get(0);

        assertSame(configuration.getCacheEntryListenerFactory().create(),
                   configuration.getCacheEntryListenerFactory().create());
    }

    @Test
    void multipleEventsInOneBatchAreAllForwarded() {
        List<Object> created = new ArrayList<>();
        testSubject.registerCacheEntryListener(new Cache.EntryListenerAdapter() {
            @Override
            public void onEntryCreated(Object key, Object value) {
                created.add(key);
            }
        });
        Object listener = registeredConfigurations.get(0).getCacheEntryListenerFactory().create();

        ((CacheEntryCreatedListener<Object, Object>) listener).onCreated(
                List.of(event(EventType.CREATED, "a", 1), event(EventType.CREATED, "b", 2))
        );

        assertEquals(List.of("a", "b"), created);
    }

    private CacheEntryEvent<Object, Object> event(EventType type, Object key, Object value) {
        return new CacheEntryEvent<>(jCache, type) {
            @Override
            public Object getOldValue() {
                return null;
            }

            @Override
            public boolean isOldValueAvailable() {
                return false;
            }

            @Override
            public Object getKey() {
                return key;
            }

            @Override
            public Object getValue() {
                return value;
            }

            @Override
            public <T> T unwrap(Class<T> clazz) {
                throw new IllegalArgumentException(clazz.getName());
            }
        };
    }
}
