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

package org.axonframework.common.jdbc;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link ConnectionExecutor}.
 */
class ConnectionExecutorTest {

    private static Connection stubConnection() {
        return (Connection) Proxy.newProxyInstance(
                ConnectionExecutorTest.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    @Test
    void constructorRejectsNullProvider() {
        NullPointerException exception =
                assertThrows(NullPointerException.class, () -> new ConnectionExecutor(null));

        assertEquals("provider", exception.getMessage());
    }

    @Test
    void applyPassesProvidedConnectionToFunctionAndCompletesWithItsResult() throws Exception {
        Connection connection = stubConnection();
        ConnectionExecutor testSubject = new ConnectionExecutor(() -> connection);

        CompletableFuture<String> result = testSubject.apply(received -> {
            assertSame(connection, received);
            return "result";
        });

        assertTrue(result.isDone());
        assertEquals("result", result.get());
    }

    @Test
    void applyCompletesWithNullWhenFunctionReturnsNull() throws Exception {
        ConnectionExecutor testSubject = new ConnectionExecutor(ConnectionExecutorTest::stubConnection);

        CompletableFuture<Object> result = testSubject.apply(received -> null);

        assertNull(result.get());
    }

    @Test
    void applyCompletesExceptionallyWhenFunctionThrows() {
        ConnectionExecutor testSubject = new ConnectionExecutor(ConnectionExecutorTest::stubConnection);
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
        SQLException failure = new SQLException("no connection");
        ConnectionExecutor testSubject = new ConnectionExecutor(() -> {
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
    void acceptRunsConsumerWithProvidedConnection() throws Exception {
        Connection connection = stubConnection();
        ConnectionExecutor testSubject = new ConnectionExecutor(() -> connection);
        AtomicInteger invocations = new AtomicInteger();

        CompletableFuture<Void> result = testSubject.accept(received -> {
            assertSame(connection, received);
            invocations.incrementAndGet();
        });

        assertNull(result.get());
        assertEquals(1, invocations.get());
    }

    @Test
    void acceptPropagatesConsumerFailureThroughFuture() {
        ConnectionExecutor testSubject = new ConnectionExecutor(ConnectionExecutorTest::stubConnection);

        CompletableFuture<Void> result = testSubject.accept(received -> {
            throw new SQLException("consumer failed");
        });

        ExecutionException exception = assertThrows(ExecutionException.class, result::get);
        assertInstanceOf(SQLException.class, exception.getCause());
        assertEquals("consumer failed", exception.getCause().getMessage());
    }

    @Test
    void acceptRejectsNullConsumer() {
        ConnectionExecutor testSubject = new ConnectionExecutor(ConnectionExecutorTest::stubConnection);

        assertThrows(NullPointerException.class, () -> testSubject.accept(null));
    }

    @Test
    void applyRequestsNewConnectionFromProviderOnEveryInvocation() throws Exception {
        AtomicInteger requested = new AtomicInteger();
        ConnectionExecutor testSubject = new ConnectionExecutor(() -> {
            requested.incrementAndGet();
            return stubConnection();
        });

        testSubject.apply(received -> "first").get();
        testSubject.apply(received -> "second").get();

        assertEquals(2, requested.get());
    }
}
