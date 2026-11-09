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

package org.axonframework.common.tx;

import org.axonframework.common.annotation.Internal;
import org.axonframework.common.function.ThrowingFunction;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/**
 * A {@link TransactionalExecutor} that obtains its transactional resource from a supplier for each operation, applies
 * the given function to it and reports the outcome through the returned {@link CompletableFuture}.
 * <p>
 * Any {@link Exception} thrown while obtaining the resource or while applying the function results in a failed future
 * instead of being thrown. Obtaining, closing, committing and rolling back the resource is the responsibility of the
 * supplier's owner, typically the surrounding transaction.
 * <p>
 * This class holds the behavior shared by resource specific executors, which are expected to delegate to it rather
 * than extend it.
 *
 * @param <T> The type of the resource.
 * @author Terrence Daniels
 * @since 5.0.2
 */
@Internal
public class ResourceSupplierExecutor<T> implements TransactionalExecutor<T> {

    private final Callable<? extends T> resourceSupplier;

    /**
     * Creates a new instance that obtains its resource from the given {@code resourceSupplier}.
     *
     * @param resourceSupplier Supplies the resource for each operation, cannot be {@code null}. May throw any
     *                         {@link Exception}, which will result in a failed future.
     * @throws NullPointerException If the {@code resourceSupplier} is {@code null}.
     */
    public ResourceSupplierExecutor(Callable<? extends T> resourceSupplier) {
        this.resourceSupplier = Objects.requireNonNull(resourceSupplier, "resourceSupplier");
    }

    @Override
    public <R> CompletableFuture<R> apply(ThrowingFunction<T, R, Exception> function) {
        try {
            return CompletableFuture.completedFuture(function.apply(resourceSupplier.call()));
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
