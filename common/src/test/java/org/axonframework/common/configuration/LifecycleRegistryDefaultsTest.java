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

package org.axonframework.common.configuration;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the default methods of the {@link LifecycleRegistry}, through a registry that only implements
 * the two abstract handler registrations and records them.
 */
class LifecycleRegistryDefaultsTest {

    private final RecordingRegistry testSubject = new RecordingRegistry();
    private final Configuration configuration = (Configuration) Proxy.newProxyInstance(
            getClass().getClassLoader(), new Class<?>[]{Configuration.class},
            (proxy, method, args) -> {
                throw new UnsupportedOperationException(method.getName());
            });

    // --- start ---

    @Test
    void onStartRunnableRegistersInPhaseZero() throws Exception {
        AtomicInteger runs = new AtomicInteger();

        LifecycleRegistry result = testSubject.onStart((Runnable) runs::incrementAndGet);

        assertSame(testSubject, result);
        assertEquals(List.of(0), testSubject.startPhases);
        assertNull(testSubject.startHandlers.get(0).run(configuration).get());
        assertEquals(1, runs.get());
    }

    @Test
    void onStartRunnableWithPhaseRegistersInThatPhase() throws Exception {
        AtomicInteger runs = new AtomicInteger();

        testSubject.onStart(7, (Runnable) runs::incrementAndGet);
        testSubject.startHandlers.get(0).run(configuration).get();

        assertEquals(List.of(7), testSubject.startPhases);
        assertEquals(1, runs.get());
    }

    @Test
    void onStartRunnableThatThrowsCompletesExceptionally() {
        IllegalStateException failure = new IllegalStateException("boom");

        testSubject.onStart(1, (Runnable) () -> {
            throw failure;
        });
        CompletableFuture<?> future = testSubject.startHandlers.get(0).run(configuration);

        assertSame(failure, causeOf(future));
    }

    @Test
    void onStartSupplierReturnsTheFutureOfTheSupplier() {
        CompletableFuture<String> supplied = new CompletableFuture<>();
        Supplier<CompletableFuture<?>> supplier = () -> supplied;

        testSubject.onStart(3, supplier);

        assertEquals(List.of(3), testSubject.startPhases);
        assertSame(supplied, testSubject.startHandlers.get(0).run(configuration));
    }

    @Test
    void onStartConsumerReceivesTheConfigurationAndCompletesNormally() throws Exception {
        AtomicReference<Configuration> received = new AtomicReference<>();
        Consumer<Configuration> consumer = received::set;

        testSubject.onStart(2, consumer);
        CompletableFuture<?> future = testSubject.startHandlers.get(0).run(configuration);

        assertNull(future.get(5, TimeUnit.SECONDS));
        assertSame(configuration, received.get());
        assertEquals(List.of(2), testSubject.startPhases);
    }

    @Test
    void onStartConsumerThatThrowsCompletesExceptionally() {
        IllegalStateException failure = new IllegalStateException("boom");
        Consumer<Configuration> consumer = config -> {
            throw failure;
        };

        testSubject.onStart(2, consumer);

        assertSame(failure, causeOf(testSubject.startHandlers.get(0).run(configuration)));
    }

    @Test
    void onStartRejectsNullHandlers() {
        NullPointerException runnable =
                assertThrows(NullPointerException.class, () -> testSubject.onStart(1, (Runnable) null));
        NullPointerException supplier = assertThrows(
                NullPointerException.class, () -> testSubject.onStart(1, (Supplier<CompletableFuture<?>>) null));
        NullPointerException consumer =
                assertThrows(NullPointerException.class, () -> testSubject.onStart(1, (Consumer<Configuration>) null));

        assertEquals("The start handler must not be null.", runnable.getMessage());
        assertEquals("The start handler must not be null.", supplier.getMessage());
        assertEquals("startHandler must not be null", consumer.getMessage());
        assertTrue(testSubject.startPhases.isEmpty());
    }

    // --- shutdown ---

    @Test
    void onShutdownRunnableRegistersInPhaseZero() throws Exception {
        AtomicInteger runs = new AtomicInteger();

        LifecycleRegistry result = testSubject.onShutdown((Runnable) runs::incrementAndGet);

        assertSame(testSubject, result);
        assertEquals(List.of(0), testSubject.shutdownPhases);
        assertNull(testSubject.shutdownHandlers.get(0).run(configuration).get());
        assertEquals(1, runs.get());
    }

    @Test
    void onShutdownRunnableWithPhaseRegistersInThatPhase() throws Exception {
        AtomicInteger runs = new AtomicInteger();

        testSubject.onShutdown(7, (Runnable) runs::incrementAndGet);
        testSubject.shutdownHandlers.get(0).run(configuration).get();

        assertEquals(List.of(7), testSubject.shutdownPhases);
        assertEquals(1, runs.get());
    }

    @Test
    void onShutdownRunnableThatThrowsCompletesExceptionally() {
        IllegalStateException failure = new IllegalStateException("boom");

        testSubject.onShutdown(1, (Runnable) () -> {
            throw failure;
        });

        assertSame(failure, causeOf(testSubject.shutdownHandlers.get(0).run(configuration)));
    }

    @Test
    void onShutdownSupplierReturnsTheFutureOfTheSupplier() {
        CompletableFuture<String> supplied = new CompletableFuture<>();
        Supplier<CompletableFuture<?>> supplier = () -> supplied;

        testSubject.onShutdown(3, supplier);

        assertEquals(List.of(3), testSubject.shutdownPhases);
        assertSame(supplied, testSubject.shutdownHandlers.get(0).run(configuration));
    }

    @Test
    void onShutdownConsumerReceivesTheConfigurationAndCompletesNormally() throws Exception {
        AtomicReference<Configuration> received = new AtomicReference<>();
        Consumer<Configuration> consumer = received::set;

        testSubject.onShutdown(2, consumer);

        assertNull(testSubject.shutdownHandlers.get(0).run(configuration).get(5, TimeUnit.SECONDS));
        assertSame(configuration, received.get());
        assertEquals(List.of(2), testSubject.shutdownPhases);
    }

    @Test
    void onShutdownConsumerThatThrowsCompletesExceptionally() {
        IllegalStateException failure = new IllegalStateException("boom");
        Consumer<Configuration> consumer = config -> {
            throw failure;
        };

        testSubject.onShutdown(2, consumer);

        assertSame(failure, causeOf(testSubject.shutdownHandlers.get(0).run(configuration)));
    }

    @Test
    void onShutdownRejectsNullHandlers() {
        NullPointerException runnable =
                assertThrows(NullPointerException.class, () -> testSubject.onShutdown(1, (Runnable) null));
        NullPointerException supplier = assertThrows(
                NullPointerException.class, () -> testSubject.onShutdown(1, (Supplier<CompletableFuture<?>>) null));
        NullPointerException consumer = assertThrows(
                NullPointerException.class, () -> testSubject.onShutdown(1, (Consumer<Configuration>) null));

        assertEquals("The shutdown handler must not be null.", runnable.getMessage());
        assertEquals("The shutdown handler must not be null.", supplier.getMessage());
        assertEquals("shutdownHandler must not be null", consumer.getMessage());
        assertTrue(testSubject.shutdownPhases.isEmpty());
    }

    private static Throwable causeOf(CompletableFuture<?> future) {
        assertTrue(future.isCompletedExceptionally());
        ExecutionException exception = assertThrows(ExecutionException.class, future::get);
        return exception.getCause();
    }

    private static final class RecordingRegistry implements LifecycleRegistry {

        private final List<Integer> startPhases = new ArrayList<>();
        private final List<LifecycleHandler> startHandlers = new ArrayList<>();
        private final List<Integer> shutdownPhases = new ArrayList<>();
        private final List<LifecycleHandler> shutdownHandlers = new ArrayList<>();

        @Override
        public LifecycleRegistry registerLifecyclePhaseTimeout(long timeout, TimeUnit timeUnit) {
            return this;
        }

        @Override
        public LifecycleRegistry onStart(int phase, LifecycleHandler startHandler) {
            startPhases.add(phase);
            startHandlers.add(startHandler);
            return this;
        }

        @Override
        public LifecycleRegistry onShutdown(int phase, LifecycleHandler shutdownHandler) {
            shutdownPhases.add(phase);
            shutdownHandlers.add(shutdownHandler);
            return this;
        }
    }
}
