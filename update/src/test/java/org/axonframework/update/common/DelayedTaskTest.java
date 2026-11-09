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

package org.axonframework.update.common;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link DelayedTask}.
 */
class DelayedTaskTest {

    private static final long TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(10);

    private static void awaitTrue(BooleanSupplier condition, String description) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(TIMEOUT_MILLIS);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Timed out waiting for: " + description);
            }
            Thread.sleep(2);
        }
    }

    @Test
    void rejectsNegativeDelay() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> DelayedTask.of(() -> {
                }, -1));

        assertEquals("Delay must be non-negative.", exception.getMessage());
    }

    @Test
    void rejectsNullRunnable() {
        NullPointerException exception = assertThrows(NullPointerException.class, () -> DelayedTask.of(null, 0));

        assertEquals("The runnable must not be null.", exception.getMessage());
    }

    @Test
    void runsTheRunnableAndReportsSuccess() throws InterruptedException {
        AtomicInteger runs = new AtomicInteger();

        DelayedTask task = DelayedTask.of(runs::incrementAndGet, 0);
        awaitTrue(task::isFinished, "task to finish");

        assertEquals(1, runs.get());
        assertTrue(task.isStarted());
        assertFalse(task.isFailed());
        assertNull(task.getFailureCause());
    }

    @Test
    void waitsForTheDelayBeforeRunning() throws InterruptedException {
        long delayMillis = 200;
        long created = System.nanoTime();
        AtomicReference<Long> ranAt = new AtomicReference<>();

        DelayedTask task = DelayedTask.of(() -> ranAt.set(System.nanoTime()), delayMillis);
        awaitTrue(task::isFinished, "task to finish");

        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(ranAt.get() - created);
        assertTrue(elapsedMillis >= delayMillis - 20, "Ran after only " + elapsedMillis + "ms");
    }

    @Test
    void doesNotStartBeforeTheDelayHasElapsed() {
        DelayedTask task = DelayedTask.of(() -> {
        }, TimeUnit.MINUTES.toMillis(5));

        assertFalse(task.isStarted());
        assertFalse(task.isFinished());
        assertFalse(task.isFailed());

        task.cancel();
    }

    @Test
    void cancellingBeforeTheDelayElapsedPreventsTheRunAndReportsAnInterruption() throws InterruptedException {
        AtomicBoolean ran = new AtomicBoolean();
        DelayedTask task = DelayedTask.of(() -> ran.set(true), TimeUnit.MINUTES.toMillis(5));

        task.cancel();
        awaitTrue(task::isFinished, "cancelled task to finish");

        assertFalse(ran.get());
        assertFalse(task.isStarted());
        assertTrue(task.isFailed());
        assertInstanceOf(InterruptedException.class, task.getFailureCause());
    }

    @Test
    void reportsFailureOfTheRunnable() throws InterruptedException {
        IllegalStateException failure = new IllegalStateException("boom");

        DelayedTask task = DelayedTask.of(() -> {
            throw failure;
        }, 0);
        awaitTrue(task::isFinished, "failing task to finish");

        assertTrue(task.isStarted());
        assertTrue(task.isFailed());
        assertSame(failure, task.getFailureCause());
    }

    @Test
    void cancellingAfterCompletionChangesNothing() throws InterruptedException {
        DelayedTask task = DelayedTask.of(() -> {
        }, 0);
        awaitTrue(task::isFinished, "task to finish");

        task.cancel();

        assertFalse(task.isFailed());
        assertNull(task.getFailureCause());
    }

    @Test
    void runsOnAVirtualThreadNamedAxoniq() throws InterruptedException {
        AtomicReference<Thread> runner = new AtomicReference<>();

        DelayedTask task = DelayedTask.of(() -> runner.set(Thread.currentThread()), 0);
        awaitTrue(task::isFinished, "task to finish");

        assertTrue(runner.get().isVirtual());
        assertEquals("Axoniq", runner.get().getName());
    }

    @Test
    void failureCauseIsAlreadyAvailableAsSoonAsFailureIsReported() throws InterruptedException {
        for (int i = 0; i < 2_000; i++) {
            DelayedTask task = DelayedTask.of(() -> {
                throw new IllegalStateException("boom");
            }, 0);

            awaitTrue(task::isFailed, "task to report failure");

            assertNotNull(task.getFailureCause(), "Failure reported without a cause on iteration " + i);
        }
    }
}
