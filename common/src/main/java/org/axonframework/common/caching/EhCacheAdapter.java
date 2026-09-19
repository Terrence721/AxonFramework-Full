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

package org.axonframework.common.caching;

import org.axonframework.common.Registration;
import org.ehcache.core.Ehcache;
import org.ehcache.event.CacheEvent;
import org.ehcache.event.CacheEventListener;
import org.ehcache.event.EventFiring;
import org.ehcache.event.EventOrdering;
import org.ehcache.event.EventType;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * Cache implementation that delegates all calls to an EhCache instance.
 *
 * @since 2.1.2
 */
@SuppressWarnings("rawtypes")
public class EhCacheAdapter extends AbstractCacheAdapter<CacheEventListener> {

    private final Ehcache ehCache;

    /**
     * Initialize the adapter to forward all call to the given {@code ehCache} instance
     *
     * @param ehCache The cache instance to forward calls to
     */
    public EhCacheAdapter(Ehcache ehCache) {
        this.ehCache = ehCache;
    }

    @SuppressWarnings("unchecked")
    @Override
    public @Nullable <K, V> V get(K key) {
        final Object value = ehCache.get(key);
        return value != null ? (V) value : null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void put(Object key, Object value) {
        ehCache.put(key, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean putIfAbsent(Object key, Object value) {
        return ehCache.putIfAbsent(key, value) == null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean remove(Object key) {
        Object value = ehCache.get(key);
        if (value == null) {
            return false;
        }
        return ehCache.remove(key, value);
    }

    @Override
    public void removeAll() {
        ehCache.clear();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean containsKey(Object key) {
        return ehCache.containsKey(key);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Delegates to {@link Ehcache#replace(Object, Object, Object)}.
     */
    @SuppressWarnings("unchecked")
    @Override
    protected <V> boolean replace(Object key, V oldValue, V newValue) {
        return ehCache.replace(key, oldValue, newValue);
    }

    @Override
    protected CacheEventListener createListenerAdapter(EntryListener cacheEntryListener) {
        return new EhCacheAdapter.CacheEventListenerAdapter(cacheEntryListener);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Registration doRegisterListener(CacheEventListener listenerAdapter) {
        ehCache.getRuntimeConfiguration().registerCacheEventListener(
                listenerAdapter,
                EventOrdering.ORDERED,
                EventFiring.SYNCHRONOUS,
                EnumSet.allOf(EventType.class)
        );
        return () -> {
            try {
                ehCache.getRuntimeConfiguration().deregisterCacheEventListener(listenerAdapter);
            } catch (IllegalStateException e) {
                return false;
            }
            return true;
        };
    }

    private static class CacheEventListenerAdapter implements CacheEventListener {

        private final EntryListener delegate;

        public CacheEventListenerAdapter(EntryListener delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onEvent(CacheEvent event) {
            switch (event.getType()) {
                case CREATED:
                    delegate.onEntryCreated(event.getKey(), event.getNewValue());
                    break;
                case UPDATED:
                    delegate.onEntryUpdated(event.getKey(), event.getNewValue());
                    break;
                case REMOVED:
                case EVICTED:
                    delegate.onEntryRemoved(event.getKey());
                    break;
                case EXPIRED:
                    delegate.onEntryExpired(event.getKey());
                    break;
                default:
                    throw new AssertionError("Unsupported event type " + event.getType());
            }
        }
    }
}
