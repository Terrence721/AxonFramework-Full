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

package org.axonframework.common.lifecycle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link ShutdownLatch}.
 */
class ShutdownLatchTest {

    private ShutdownLatch testSubject;

    @BeforeEach
    void setUp() {
        testSubject = new ShutdownLatch();
    }

    @Test
    void isNotShuttingDownInitially() {
        assertFalse(testSubject.isShuttingDown());
        assertDoesNotThrow(() -> testSubject.ifShuttingDown("not shutting down"));
    }

    @Test
    void initiateShutdownCompletesImmediatelyWithoutActivities() {
        CompletableFuture<Void> result = testSubject.initiateShutdown();

        assertTrue(result.isDone());
        assertTrue(testSubject.isShuttingDown());
    }

    @Test
    void initiateShutdownWaitsForAllRegisteredActivitiesToEnd() {
        ShutdownLatch.ActivityHandle first = testSubject.registerActivity();
        ShutdownLatch.ActivityHandle second = testSubject.registerActivity();

        CompletableFuture<Void> result = testSubject.initiateShutdown();
        assertFalse(result.isDone());

        first.end();
        assertFalse(result.isDone());

        second.end();
        assertTrue(result.isDone());
    }

    @Test
    void endingAnActivityTwiceOnlyCountsOnce() {
        ShutdownLatch.ActivityHandle first = testSubject.registerActivity();
        testSubject.registerActivity();

        CompletableFuture<Void> result = testSubject.initiateShutdown();
        first.end();
        first.end();

        assertFalse(result.isDone());
    }

    @Test
    void closeEndsTheActivity() {
        CompletableFuture<Void> result;
        try (ShutdownLatch.ActivityHandle ignored = testSubject.registerActivity()) {
            assertNotNull(ignored);
            result = testSubject.initiateShutdown();
            assertFalse(result.isDone());
        }

        assertTrue(result.isDone());
    }

    @Test
    void registerActivityFailsOnceShutdownWasInitiated() {
        testSubject.initiateShutdown();

        assertThrows(ShutdownInProgressException.class, () -> testSubject.registerActivity());
    }

    @Test
    void failedRegistrationDoesNotCountAsActivity() {
        CompletableFuture<Void> shutdown = testSubject.initiateShutdown();
        assertThrows(ShutdownInProgressException.class, () -> testSubject.registerActivity());

        assertTrue(shutdown.isDone());
    }

    @Test
    void initiateShutdownTwiceReturnsTheSameFuture() {
        testSubject.registerActivity();

        CompletableFuture<Void> first = testSubject.initiateShutdown();
        CompletableFuture<Void> second = testSubject.initiateShutdown();

        assertSame(first, second);
    }

    @Test
    void ifShuttingDownThrowsExceptionWithGivenMessage() {
        testSubject.initiateShutdown();

        ShutdownInProgressException exception =
                assertThrows(ShutdownInProgressException.class, () -> testSubject.ifShuttingDown("custom message"));

        assertEquals("custom message", exception.getMessage());
    }

    @Test
    void ifShuttingDownThrowsExceptionFromSupplier() {
        testSubject.initiateShutdown();
        IllegalStateException expected = new IllegalStateException("from supplier");

        IllegalStateException thrown =
                assertThrows(IllegalStateException.class, () -> testSubject.ifShuttingDown(() -> expected));

        assertSame(expected, thrown);
    }

    @Test
    void initializeCancelsPendingShutdownAndReopensLatch() {
        testSubject.registerActivity();
        CompletableFuture<Void> pendingShutdown = testSubject.initiateShutdown();

        testSubject.initialize();

        assertTrue(pendingShutdown.isCancelled());
        assertFalse(testSubject.isShuttingDown());
        assertDoesNotThrow(() -> testSubject.registerActivity());
    }

    @Test
    void initializeOnFreshLatchIsHarmless() {
        testSubject.initialize();

        assertFalse(testSubject.isShuttingDown());
    }
}
