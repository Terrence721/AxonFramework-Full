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

package org.axonframework.common.lock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link PessimisticLockFactory}.
 */
class PessimisticLockFactoryTest {

    private final ExecutorService executor = Executors.newCachedThreadPool();

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    private <T> Future<T> inOtherThread(Callable<T> task) {
        return executor.submit(task);
    }

    private static Throwable failureOf(Future<?> future) {
        ExecutionException exception = assertThrows(ExecutionException.class, () -> future.get(10, TimeUnit.SECONDS));
        return exception.getCause();
    }

    @Test
    void obtainedLockIsHeldUntilReleased() {
        LockFactory testSubject = PessimisticLockFactory.usingDefaults();

        Lock lock = testSubject.obtainLock("aggregate");
        assertTrue(lock.isHeld());

        lock.release();
        assertFalse(lock.isHeld());
    }

    @Test
    void obtainLockRejectsNullIdentifier() {
        LockFactory testSubject = PessimisticLockFactory.usingDefaults();

        assertThrows(IllegalArgumentException.class, () -> testSubject.obtainLock(null));
    }

    @Test
    void closeReleasesTheLock() {
        LockFactory testSubject = PessimisticLockFactory.usingDefaults();

        Lock lock = testSubject.obtainLock("aggregate");
        try (lock) {
            assertTrue(lock.isHeld());
        }

        assertFalse(lock.isHeld());
    }

    @Test
    void sameThreadCanHoldTheSameLockMultipleTimesAndNeedsMatchingReleases() throws Exception {
        PessimisticLockFactory testSubject = PessimisticLockFactory.builder()
                                                                   .acquireAttempts(3)
                                                                   .lockAttemptTimeout(1)
                                                                   .build();
        Lock first = testSubject.obtainLock("aggregate");
        Lock second = testSubject.obtainLock("aggregate");

        first.release();
        assertTrue(second.isHeld());
        assertInstanceOf(LockAcquisitionFailedException.class,
                         failureOf(inOtherThread(() -> testSubject.obtainLock("aggregate"))));

        second.release();
        assertFalse(second.isHeld());
        assertDoesNotThrow(() -> inOtherThread(() -> {
            testSubject.obtainLock("aggregate").release();
            return null;
        }).get(10, TimeUnit.SECONDS));
    }

    @Test
    void locksForDifferentIdentifiersDoNotBlockEachOther() throws Exception {
        PessimisticLockFactory testSubject = PessimisticLockFactory.builder()
                                                                   .acquireAttempts(3)
                                                                   .lockAttemptTimeout(1)
                                                                   .build();
        Lock lock = testSubject.obtainLock("one");

        Boolean otherHeld = inOtherThread(() -> {
            Lock other = testSubject.obtainLock("two");
            boolean held = other.isHeld();
            other.release();
            return held;
        }).get(10, TimeUnit.SECONDS);

        assertTrue(otherHeld);
        lock.release();
    }

    @Test
    void lockIsReusableAfterRelease() {
        LockFactory testSubject = PessimisticLockFactory.usingDefaults();

        testSubject.obtainLock("aggregate").release();
        Lock again = testSubject.obtainLock("aggregate");

        assertTrue(again.isHeld());
        again.release();
    }

    @Test
    void releasingFromThreadThatDoesNotHoldTheLockFails() {
        LockFactory testSubject = PessimisticLockFactory.usingDefaults();
        Lock lock = testSubject.obtainLock("aggregate");

        Throwable failure = failureOf(inOtherThread(() -> {
            lock.release();
            return null;
        }));

        assertInstanceOf(IllegalMonitorStateException.class, failure);
        assertTrue(lock.isHeld());
        lock.release();
    }

    @Test
    void obtainLockFailsAfterMaximumAttemptsWhenHeldByAnotherThread() {
        PessimisticLockFactory testSubject = PessimisticLockFactory.builder()
                                                                   .acquireAttempts(3)
                                                                   .lockAttemptTimeout(1)
                                                                   .build();
        Lock held = testSubject.obtainLock("aggregate");

        Throwable failure = failureOf(inOtherThread(() -> testSubject.obtainLock("aggregate")));

        assertInstanceOf(LockAcquisitionFailedException.class, failure);
        assertTrue(failure.getMessage().contains("aggregate"));
        assertTrue(failure.getMessage().contains("maximum attempts exceeded (3)"));
        held.release();
    }

    @Test
    void obtainLockSucceedsWhenHolderReleasesWithinTheAttemptWindow() throws Exception {
        PessimisticLockFactory testSubject = PessimisticLockFactory.builder()
                                                                   .acquireAttempts(1000)
                                                                   .lockAttemptTimeout(10)
                                                                   .build();
        Lock held = testSubject.obtainLock("aggregate");
        CountDownLatch waiting = new CountDownLatch(1);

        Future<Boolean> acquired = inOtherThread(() -> {
            waiting.countDown();
            Lock lock = testSubject.obtainLock("aggregate");
            boolean isHeld = lock.isHeld();
            lock.release();
            return isHeld;
        });
        assertTrue(waiting.await(10, TimeUnit.SECONDS));
        Thread.sleep(100);
        held.release();

        assertTrue(acquired.get(10, TimeUnit.SECONDS));
    }

    @Test
    void obtainLockFailsImmediatelyWhenQueueThresholdIsReached() throws Exception {
        PessimisticLockFactory testSubject = PessimisticLockFactory.builder()
                                                                   .queueLengthThreshold(1)
                                                                   .acquireAttempts(1000)
                                                                   .lockAttemptTimeout(10)
                                                                   .build();
        Lock held = testSubject.obtainLock("aggregate");
        CountDownLatch queued = new CountDownLatch(1);
        Future<?> waiter = inOtherThread(() -> {
            queued.countDown();
            testSubject.obtainLock("aggregate").release();
            return null;
        });
        assertTrue(queued.await(10, TimeUnit.SECONDS));
        Thread.sleep(200);

        Throwable failure = failureOf(inOtherThread(() -> testSubject.obtainLock("aggregate")));

        assertInstanceOf(LockAcquisitionFailedException.class, failure);
        assertTrue(failure.getMessage().contains("too many queued threads"));
        held.release();
        waiter.get(10, TimeUnit.SECONDS);
    }

    @Test
    void interruptedThreadFailsAcquisitionAndKeepsInterruptFlag() throws Exception {
        PessimisticLockFactory testSubject = PessimisticLockFactory.builder()
                                                                   .acquireAttempts(1000)
                                                                   .lockAttemptTimeout(10)
                                                                   .build();
        Lock held = testSubject.obtainLock("aggregate");

        Future<Boolean> interrupted = inOtherThread(() -> {
            Thread.currentThread().interrupt();
            try {
                testSubject.obtainLock("aggregate");
                return false;
            } catch (LockAcquisitionFailedException e) {
                return "Thread was interrupted".equals(e.getMessage()) && Thread.currentThread().isInterrupted();
            }
        });

        assertTrue(interrupted.get(10, TimeUnit.SECONDS));
        held.release();
    }

    @Test
    void detectsImminentDeadlockBetweenTwoThreads() throws Exception {
        PessimisticLockFactory testSubject = PessimisticLockFactory.builder()
                                                                   .acquireAttempts(1000)
                                                                   .lockAttemptTimeout(10)
                                                                   .build();
        CountDownLatch bothHoldingOwnLock = new CountDownLatch(2);

        Callable<Throwable> firstThread = () -> crossLock(testSubject, "A", "B", bothHoldingOwnLock);
        Callable<Throwable> secondThread = () -> crossLock(testSubject, "B", "A", bothHoldingOwnLock);
        Future<Throwable> first = inOtherThread(firstThread);
        Future<Throwable> second = inOtherThread(secondThread);

        Throwable firstResult = first.get(30, TimeUnit.SECONDS);
        Throwable secondResult = second.get(30, TimeUnit.SECONDS);

        assertTrue(firstResult instanceof DeadlockException || secondResult instanceof DeadlockException,
                   "Expected at least one thread to detect the deadlock but got " + firstResult + " / " + secondResult);
    }

    /**
     * Obtains {@code own}, waits for the other thread to hold its own lock, then tries to obtain {@code other}. Returns
     * the failure of that attempt or {@code null} on success. Always releases what it holds, so the other thread can
     * finish once one of them backed off.
     */
    private static Throwable crossLock(PessimisticLockFactory factory, String own, String other, CountDownLatch latch)
            throws InterruptedException {
        Lock ownLock = factory.obtainLock(own);
        try {
            latch.countDown();
            latch.await();
            Lock otherLock = factory.obtainLock(other);
            otherLock.release();
            return null;
        } catch (LockAcquisitionFailedException e) {
            return e;
        } finally {
            ownLock.release();
        }
    }

    @Test
    void separateFactoriesDoNotShareLocks() throws Exception {
        PessimisticLockFactory first = PessimisticLockFactory.usingDefaults();
        PessimisticLockFactory second = PessimisticLockFactory.usingDefaults();
        Lock held = first.obtainLock("aggregate");

        Lock other = inOtherThread(() -> second.obtainLock("aggregate")).get(10, TimeUnit.SECONDS);

        assertNotSame(held, other);
        held.release();
    }

    @Test
    void builderRejectsInvalidAcquireAttempts() {
        PessimisticLockFactory.Builder builder = PessimisticLockFactory.builder();

        assertThrows(IllegalArgumentException.class, () -> builder.acquireAttempts(0));
        assertThrows(IllegalArgumentException.class, () -> builder.acquireAttempts(-2));
        assertDoesNotThrow(() -> builder.acquireAttempts(1));
    }

    @Test
    void builderRejectsMinusOneAcquireAttemptsWithClearMessage() {
        PessimisticLockFactory.Builder builder = PessimisticLockFactory.builder();

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> builder.acquireAttempts(-1));

        assertEquals("acquireAttempts needs to be a positive integer, but was '-1'", exception.getMessage());
    }

    @Test
    void builderRejectsInvalidQueueLengthThreshold() {
        PessimisticLockFactory.Builder builder = PessimisticLockFactory.builder();

        assertThrows(IllegalArgumentException.class, () -> builder.queueLengthThreshold(0));
        assertThrows(IllegalArgumentException.class, () -> builder.queueLengthThreshold(-1));
        assertDoesNotThrow(() -> builder.queueLengthThreshold(1));
    }

    @Test
    void builderRejectsNegativeLockAttemptTimeout() {
        PessimisticLockFactory.Builder builder = PessimisticLockFactory.builder();

        assertThrows(IllegalArgumentException.class, () -> builder.lockAttemptTimeout(-1));
        assertDoesNotThrow(() -> builder.lockAttemptTimeout(0));
    }

    @Test
    void builderMethodsAreChainable() {
        PessimisticLockFactory.Builder builder = PessimisticLockFactory.builder();

        assertEquals(builder, builder.acquireAttempts(5).queueLengthThreshold(5).lockAttemptTimeout(5));
    }
}
