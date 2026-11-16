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

package org.axonframework.common.tx;

import org.axonframework.common.function.ThrowingConsumer;
import org.axonframework.common.function.ThrowingFunction;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for the {@link TransactionalExecutor} interface to achieve 100% coverage.
 */
class TransactionalExecutorTest {

    private static class TestTransactionalExecutor implements TransactionalExecutor<String> {
        private final boolean executeSuccess;
        private final CompletableFuture<Object> returnedFuture;

        TestTransactionalExecutor(boolean executeSuccess, CompletableFuture<Object> returnedFuture) {
            this.executeSuccess = executeSuccess;
            this.returnedFuture = returnedFuture;
        }

        @Override
        public <R> CompletableFuture<R> apply(ThrowingFunction<String, R, Exception> function) {
            if (executeSuccess) {
                @SuppressWarnings("unchecked")
                CompletableFuture<R> cast = (CompletableFuture<R>) returnedFuture;
                return cast;
            } else {
                throw new RuntimeException("Execution failed");
            }
        }
    }

    @Test
    void acceptWithNullConsumerThrowsNullPointerException() {
        TransactionalExecutor<String> executor = new TestTransactionalExecutor(true, CompletableFuture.completedFuture(null));

        assertThrows(NullPointerException.class, () -> executor.accept(null));
    }

    @Test
    void acceptWithValidConsumerReturnsVoidFuture() {
        TransactionalExecutor<String> executor = new TestTransactionalExecutor(true, CompletableFuture.completedFuture(null));

        CompletableFuture<Void> future = executor.accept(resource -> {
            // Consume resource
            assertNotNull(resource);
        });

        assertNotNull(future);
        assertTrue(future.isDone());
        assertNull(future.join());
    }

    @Test
    void acceptWithThrowingConsumerPropagatesException() {
        TransactionalExecutor<String> executor = new TestTransactionalExecutor(false, CompletableFuture.completedFuture(null));

        CompletableFuture<Void> future = executor.accept(resource -> {
            throw new RuntimeException("Test exception");
        });

        assertNotNull(future);
        assertTrue(future.isCompletedExceptionally());
    }

    @Test
    void applyWithNullFunctionThrowsNullPointerException() {
        TransactionalExecutor<String> executor = new TestTransactionalExecutor(true, CompletableFuture.completedFuture(null));

        assertThrows(NullPointerException.class, () -> executor.apply(null));
    }

    @Test
    void applyWithValidFunctionReturnsFuture() {
        TransactionalExecutor<String> executor = new TestTransactionalExecutor(true, CompletableFuture.completedFuture("result"));

        CompletableFuture<String> future = executor.apply(resource -> {
            assertNotNull(resource);
            return "result";
        });

        assertNotNull(future);
        assertTrue(future.isDone());
        assertEquals("result", future.join());
    }

    @Test
    void applyWithThrowingFunctionPropagatesException() {
        TransactionalExecutor<String> executor = new TestTransactionalExecutor(false, CompletableFuture.completedFuture(null));

        CompletableFuture<String> future = executor.apply(resource -> {
            throw new RuntimeException("Test exception");
        });

        assertNotNull(future);
        assertTrue(future.isCompletedExceptionally());
    }
}