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

package org.axonframework.common.jpa;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link EntityManagerExecutor}.
 */
class EntityManagerExecutorTest {

    private static EntityManager stubEntityManager() {
        return (EntityManager) Proxy.newProxyInstance(
                EntityManagerExecutorTest.class.getClassLoader(),
                new Class<?>[]{EntityManager.class},
                (proxy, method, args) -> {
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    @Test
    void constructorRejectsNullProvider() {
        NullPointerException exception =
                assertThrows(NullPointerException.class, () -> new EntityManagerExecutor(null));

        assertEquals("provider", exception.getMessage());
    }

    @Test
    void applyPassesProvidedEntityManagerToFunctionAndCompletesWithItsResult() throws Exception {
        EntityManager entityManager = stubEntityManager();
        EntityManagerExecutor testSubject = new EntityManagerExecutor(() -> entityManager);

        CompletableFuture<String> result = testSubject.apply(received -> {
            assertSame(entityManager, received);
            return "result";
        });

        assertTrue(result.isDone());
        assertEquals("result", result.get());
    }

    @Test
    void applyCompletesWithNullWhenFunctionReturnsNull() throws Exception {
        EntityManagerExecutor testSubject = new EntityManagerExecutor(EntityManagerExecutorTest::stubEntityManager);

        assertNull(testSubject.apply(received -> null).get());
    }

    @Test
    void applyCompletesExceptionallyWhenFunctionThrows() {
        EntityManagerExecutor testSubject = new EntityManagerExecutor(EntityManagerExecutorTest::stubEntityManager);
        IllegalStateException failure = new IllegalStateException("boom");

        CompletableFuture<Object> result = testSubject.apply(received -> {
            throw failure;
        });

        assertTrue(result.isCompletedExceptionally());
        ExecutionException exception = assertThrows(ExecutionException.class, result::get);
        assertSame(failure, exception.getCause());
    }

    @Test
    void applyCompletesExceptionallyWhenProviderFailsAndNeverInvokesFunction() {
        IllegalStateException failure = new IllegalStateException("no entity manager");
        EntityManagerExecutor testSubject = new EntityManagerExecutor(() -> {
            throw failure;
        });
        AtomicInteger invocations = new AtomicInteger();

        CompletableFuture<Object> result = testSubject.apply(received -> {
            invocations.incrementAndGet();
            return "unreachable";
        });

        ExecutionException exception = assertThrows(ExecutionException.class, result::get);
        assertSame(failure, exception.getCause());
        assertEquals(0, invocations.get());
    }

    @Test
    void acceptRunsConsumerWithProvidedEntityManager() throws Exception {
        EntityManager entityManager = stubEntityManager();
        EntityManagerExecutor testSubject = new EntityManagerExecutor(() -> entityManager);
        AtomicInteger invocations = new AtomicInteger();

        CompletableFuture<Void> result = testSubject.accept(received -> {
            assertSame(entityManager, received);
            invocations.incrementAndGet();
        });

        assertNull(result.get());
        assertEquals(1, invocations.get());
    }

    @Test
    void acceptRejectsNullConsumer() {
        EntityManagerExecutor testSubject = new EntityManagerExecutor(EntityManagerExecutorTest::stubEntityManager);

        assertThrows(NullPointerException.class, () -> testSubject.accept(null));
    }
}
