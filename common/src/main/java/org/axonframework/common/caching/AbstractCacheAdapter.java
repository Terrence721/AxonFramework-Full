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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.UnaryOperator;

/**
 * Abstract implementation of the Cache interface which makes it easier to implement Adapters.
 *
 * @param <L> The type of event listener the cache uses
 * @since 2.1.2
 */
public abstract class AbstractCacheAdapter<L> implements Cache {

    private final ConcurrentMap<EntryListener, L> registeredAdapters =
            new ConcurrentHashMap<>();

    /**
     * Creates an adapter for the given {@code cacheEntryListener}. The adapter must forward all incoming
     * notifications to the respective methods on the {@code cacheEntryListener}.
     *
     * @param cacheEntryListener The listener to create an adapter for
     * @return an adapter that forwards notifications
     */
    protected abstract L createListenerAdapter(EntryListener cacheEntryListener);

    @Override
    public Registration registerCacheEntryListener(EntryListener entryListener) {
        L adapter = createListenerAdapter(entryListener);
        Registration registration
                = registeredAdapters.putIfAbsent(entryListener, adapter) == null ? doRegisterListener(adapter) : null;
        return () -> {
            L removedAdapter = registeredAdapters.remove(entryListener);
            if (removedAdapter != null) {
                if (registration != null) {
                    registration.cancel();
                }
                return true;
            }
            return false;
        };
    }

    /**
     * Registers the given listener with the cache implementation
     *
     * @param listenerAdapter the listener to register
     * @return a handle to deregister the listener
     */
    protected abstract Registration doRegisterListener(L listenerAdapter);

    @SuppressWarnings("unchecked")
    @Override
    public <V> void computeIfPresent(Object key, UnaryOperator<V> update) {
        Object oldValue;
        V newValue;
        do {
            oldValue = get(key);
            if (oldValue == null) {
                break;
            }
            newValue = update.apply((V) oldValue);
        } while (!replaceOrRemove(key, (V) oldValue, newValue));
    }

    /**
     * Replace or remove the element under {@code key}. If the {@code newValue} is not {@code null}, we invoke
     * {@link #replace(Object, Object, Object)}. If the {@code newValue} is {@code null}, the compute task decided to
     * remove the entry instead.
     *
     * @param key      The reference to the value to replace or remove, depending on whether the {@code newValue} is
     *                 {@code null}.
     * @param oldValue The old entry to replace with the {@code newValue}, if {@code newValue} is not {@code null}.
     * @param newValue The new value to replace with the {@code oldValue}, if it is not {@code null}.
     * @param <V>      The generic type of the value stored under the given {@code key}.
     * @return A boolean stating whether the {@link #replace(Object, Object, Object)} or {@link #remove(Object)} task
     * succeeded.
     */
    private <V> boolean replaceOrRemove(Object key, V oldValue, V newValue) {
        return newValue != null ? replace(key, oldValue, newValue) : remove(key);
    }

    /**
     * Replaces the value under {@code key} with {@code newValue}, but only if it currently holds {@code oldValue}.
     * Implementations should delegate to their underlying cache provider's own compare-and-replace operation.
     *
     * @param key      The reference to the value to replace.
     * @param oldValue The value expected to currently be associated with {@code key}.
     * @param newValue The value to replace it with.
     * @param <V>      The generic type of the value stored under the given {@code key}.
     * @return {@code true} if the replacement succeeded, {@code false} otherwise.
     */
    protected abstract <V> boolean replace(Object key, V oldValue, V newValue);
}
