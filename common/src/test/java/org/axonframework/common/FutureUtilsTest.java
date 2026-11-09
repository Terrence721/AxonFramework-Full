package org.axonframework.common;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FutureUtilsTest {

    @Test
    void ignoreResult() {
        assertNull(FutureUtils.ignoreResult("value"));
    }

    @Test
    void emptyCompletedFuture() {
        assertTrue(FutureUtils.emptyCompletedFuture().isDone());
        assertNull(FutureUtils.emptyCompletedFuture().join());
    }

    @Test
    void allOrEmptyEmpty() {
        assertTrue(FutureUtils.allOrEmpty(List.of()).isDone());
    }

    @Test
    void allOrEmptyNonEmpty() {
        CompletableFuture<Void> future = FutureUtils.allOrEmpty(
                List.of(CompletableFuture.completedFuture(null)));
        future.join();
    }

    @Test
    void allOrEmptyExceptionally() {
        CompletableFuture<Void> failing = new CompletableFuture<>();
        failing.completeExceptionally(new RuntimeException("boom"));
        CompletableFuture<Void> future = FutureUtils.allOrEmpty(List.of(failing));
        assertThrows(CompletionException.class, future::join);
    }

    @Test
    void alsoCompleteSuccess() {
        CompletableFuture<String> source = new CompletableFuture<>();
        CompletableFuture<String> target = new CompletableFuture<>();
        source.whenComplete(FutureUtils.alsoComplete(target));
        source.complete("value");
        assertEquals(target.join(), "value");
    }

    @Test
    void alsoCompleteExceptionally() {
        CompletableFuture<String> source = new CompletableFuture<>();
        CompletableFuture<String> target = new CompletableFuture<>();
        source.whenComplete(FutureUtils.alsoComplete(target));
        source.completeExceptionally(new RuntimeException("boom"));
        assertThrows(CompletionException.class, target::join);
    }

    @Test
    void unwrap() {
        Throwable plain = new RuntimeException("plain");
        assertSame(plain, FutureUtils.unwrap(plain));

        CompletionException ce = new CompletionException(plain);
        assertSame(plain, FutureUtils.unwrap(ce));

        ExecutionException ee = new ExecutionException(plain);
        assertSame(plain, FutureUtils.unwrap(ee));
    }

    @Test
    void runFailingSuccess() {
        CompletableFuture<String> result = FutureUtils.runFailing(() -> CompletableFuture.completedFuture("value"));
        assertEquals("value", result.join());
    }

    @Test
    void runFailingException() {
        CompletableFuture<String> result = FutureUtils.runFailing(() -> {
            throw new IllegalStateException("boom");
        });
        assertTrue(result.isCompletedExceptionally());
    }

    @Test
    void joinAndUnwrapSuccess() {
        assertEquals("value", FutureUtils.joinAndUnwrap(CompletableFuture.completedFuture("value")));
    }

    @Test
    void joinAndUnwrapException() {
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(new IllegalStateException("boom"));
        assertThrows(IllegalStateException.class, () -> FutureUtils.joinAndUnwrap(future));
    }

    @Test
    void joinAndUnwrapTimeout() {
        assertThrows(TimeoutException.class, () ->
                FutureUtils.joinAndUnwrap(new CompletableFuture<>(), Duration.ofNanos(1)));
    }

    private static void assertEquals(Object expected, Object actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
}