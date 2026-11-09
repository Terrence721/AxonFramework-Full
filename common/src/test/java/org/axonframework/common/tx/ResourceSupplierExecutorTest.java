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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link ResourceSupplierExecutor}.
 */
class ResourceSupplierExecutorTest {

    @Test
    void constructorRejectsNullSupplier() {
        NullPointerException exception =
                assertThrows(NullPointerException.class, () -> new ResourceSupplierExecutor<String>(null));

        assertEquals("resourceSupplier", exception.getMessage());
    }

    @Test
    void applyPassesSuppliedResourceToFunctionAndCompletesWithItsResult() throws Exception {
        ResourceSupplierExecutor<String> testSubject = new ResourceSupplierExecutor<>(() -> "resource");

        CompletableFuture<Integer> result = testSubject.apply(String::length);

        assertTrue(result.isDone());
        assertEquals(8, result.get());
    }

    @Test
    void applyCompletesWithNullWhenFunctionReturnsNull() throws Exception {
        ResourceSupplierExecutor<String> testSubject = new ResourceSupplierExecutor<>(() -> "resource");

        assertNull(testSubject.apply(resource -> null).get());
    }

    @Test
    void applyCompletesExceptionallyWhenFunctionThrows() {
        ResourceSupplierExecutor<String> testSubject = new ResourceSupplierExecutor<>(() -> "resource");
        IOException failure = new IOException("function failed");

        CompletableFuture<Object> result = testSubject.apply(resource -> {
            throw failure;
        });

        assertTrue(result.isCompletedExceptionally());
        ExecutionException exception = assertThrows(ExecutionException.class, result::get);
        assertSame(failure, exception.getCause());
    }

    @Test
    void applyCompletesExceptionallyWhenSupplierThrowsAndNeverInvokesFunction() {
        IOException failure = new IOException("no resource");
        ResourceSupplierExecutor<String> testSubject = new ResourceSupplierExecutor<>(() -> {
            throw failure;
        });
        AtomicInteger invocations = new AtomicInteger();

        CompletableFuture<Object> result = testSubject.apply(resource -> {
            invocations.incrementAndGet();
            return "unreachable";
        });

        ExecutionException exception = assertThrows(ExecutionException.class, result::get);
        assertSame(failure, exception.getCause());
        assertEquals(0, invocations.get());
    }

    @Test
    void applyAsksSupplierForResourceOnEveryInvocation() throws Exception {
        AtomicInteger supplied = new AtomicInteger();
        ResourceSupplierExecutor<Integer> testSubject = new ResourceSupplierExecutor<>(supplied::incrementAndGet);

        assertEquals(1, testSubject.apply(resource -> resource).get());
        assertEquals(2, testSubject.apply(resource -> resource).get());
    }

    @Test
    void acceptRunsConsumerWithSuppliedResource() throws Exception {
        ResourceSupplierExecutor<String> testSubject = new ResourceSupplierExecutor<>(() -> "resource");
        AtomicInteger invocations = new AtomicInteger();

        CompletableFuture<Void> result = testSubject.accept(resource -> {
            assertEquals("resource", resource);
            invocations.incrementAndGet();
        });

        assertNull(result.get());
        assertEquals(1, invocations.get());
    }
}
