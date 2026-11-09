package org.axonframework.common;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessUtilsTest {

    @Test
    void executeWithRetrySucceedsImmediately() {
        ProcessUtils.executeWithRetry(() -> {
        }, e -> true, 100, TimeUnit.MILLISECONDS, 0);
    }

    @Test
    void executeWithRetryRetriesAndSucceeds() {
        AtomicInteger attempts = new AtomicInteger();
        ProcessUtils.executeWithRetry(() -> {
            if (attempts.incrementAndGet() < 2) {
                throw new RuntimeException("retry");
            }
        }, e -> true, 1000, TimeUnit.MILLISECONDS, 0);

        assertTrue(attempts.get() >= 2);
    }

    @Test
    void executeWithRetryThrowsNonRetryableImmediately() {
        AtomicInteger attempts = new AtomicInteger();
        assertThrows(RuntimeException.class, () -> ProcessUtils.executeWithRetry(() -> {
            attempts.incrementAndGet();
            throw new RuntimeException("no retry");
        }, e -> false, 1000, TimeUnit.MILLISECONDS, 0));

        assertEquals(1, attempts.get());
    }

    @Test
    void executeWithRetryThrowsLastWhenExhausted() {
        assertThrows(RuntimeException.class, () -> ProcessUtils.executeWithRetry(() -> {
            throw new RuntimeException("boom");
        }, e -> true, 1, TimeUnit.MILLISECONDS, 0));
    }

    @Test
    void executeUntilTrueBooleanSupplierSuccess() {
        AtomicInteger counter = new AtomicInteger();
        ProcessUtils.executeUntilTrue(() -> counter.incrementAndGet() >= 2, 0, 5);
        assertTrue(counter.get() >= 2);
    }

    @Test
    void executeUntilTrueBooleanSupplierExhausted() {
        assertThrows(ProcessRetriesExhaustedException.class, () ->
                ProcessUtils.executeUntilTrue(() -> false, 0, 2));
    }

    @Test
    void executeUntilTrueAsyncSuccess() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        java.util.concurrent.ExecutorService executor = Executors.newSingleThreadExecutor();
        ProcessUtils.executeUntilTrue(
                () -> CompletableFuture.completedFuture(counter.incrementAndGet() >= 2),
                0, 5, executor
        ).get(5, TimeUnit.SECONDS);
        executor.shutdown();
    }

    @Test
    void executeUntilTrueAsyncExhausted() {
        java.util.concurrent.ExecutorService executor = Executors.newSingleThreadExecutor();
        assertThrows(Exception.class, () -> ProcessUtils.executeUntilTrue(
                () -> CompletableFuture.completedFuture(false),
                0, 2, executor
        ).get(5, TimeUnit.SECONDS));
        executor.shutdown();
    }

    private static void assertEquals(int expected, int actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
}