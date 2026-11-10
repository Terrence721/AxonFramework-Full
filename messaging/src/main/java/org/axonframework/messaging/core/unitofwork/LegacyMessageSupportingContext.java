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

package org.axonframework.messaging.core.unitofwork;

import org.jspecify.annotations.Nullable;
import org.axonframework.common.configuration.ComponentNotFoundException;
import org.axonframework.messaging.core.Message;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Legacy implementation of the {@link ProcessingContext} that should only be used for legacy components. Can only be
 * constructed with a {@link org.axonframework.messaging.core.Message} as parameter that is added to the resources. Any
 * lifecycle operation will fail.
 *
 * @since 5.0.0
 * @deprecated Only in use for legacy "sync" components.
 */
@Deprecated
public class LegacyMessageSupportingContext implements ProcessingContext {

    private static final String UNSUPPORTED_MESSAGE = "Cannot register lifecycle actions in this ProcessingContext";
    private final ConcurrentMap<ResourceKey<?>, Object> resources;

    /**
     * Initialize the {@link ProcessingContext} with the given {@code message} as the only resource.
     *
     * @param message The message to be used as the only resource in this context.
     */
    @Deprecated
    public LegacyMessageSupportingContext(Message message) {
        this.resources = new ConcurrentHashMap<>();
        Message.addToContext(this, message);
    }

    @Deprecated
    @Override
    public boolean isStarted() {
        return true;
    }

    @Deprecated
    @Override
    public boolean isError() {
        return false;
    }

    @Deprecated
    @Override
    public boolean isCommitted() {
        return false;
    }

    @Deprecated
    @Override
    public boolean isCompleted() {
        return false;
    }

    @Deprecated
    @Override
    public ProcessingLifecycle on(Phase phase, Function<ProcessingContext, CompletableFuture<?>> action) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Deprecated
    @Override
    public ProcessingLifecycle onError(ErrorHandler action) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Deprecated
    @Override
    public ProcessingLifecycle whenComplete(Consumer<ProcessingContext> action) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Deprecated
    @Override
    public boolean containsResource(ResourceKey<?> key) {
        return resources.containsKey(key);
    }

    @Deprecated
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getResource(ResourceKey<T> key) {
        return (T) resources.get(key);
    }

    @Deprecated
    @Override
    public Map<ResourceKey<?>, Object> resources() {
        return Map.copyOf(resources);
    }

    @Deprecated
    @Override
    @SuppressWarnings("unchecked")
    public <T> T putResource(ResourceKey<T> key,
                             T resource) {
        return (T) resources.put(key, resource);
    }

    @Deprecated
    @Override
    @SuppressWarnings("unchecked")
    public <T> T updateResource(ResourceKey<T> key,
                                UnaryOperator<T> resourceUpdater) {
        return (T) resources.compute(key, (k, v) -> resourceUpdater.apply((T) v));
    }

    @Deprecated
    @Override
    @SuppressWarnings("unchecked")
    public <T> T putResourceIfAbsent(ResourceKey<T> key,
                                     T resource) {
        return (T) resources.putIfAbsent(key, resource);
    }

    @Deprecated
    @Override
    @SuppressWarnings("unchecked")
    public <T> T computeResourceIfAbsent(ResourceKey<T> key,
                                         Supplier<T> resourceSupplier) {
        return (T) resources.computeIfAbsent(key, t -> resourceSupplier.get());
    }

    @Deprecated
    @Override
    @SuppressWarnings("unchecked")
    public <T> T removeResource(ResourceKey<T> key) {
        return (T) resources.remove(key);
    }

    @Deprecated
    @Override
    public <T> boolean removeResource(ResourceKey<T> key,
                                      T expectedResource) {
        return resources.remove(key, expectedResource);
    }

    @Deprecated
    @Override
    public <C> C component(Class<C> type, @Nullable String name) {
        throw new ComponentNotFoundException(type, name);
    }
}
