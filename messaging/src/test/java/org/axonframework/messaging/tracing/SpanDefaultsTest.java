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

package org.axonframework.messaging.tracing;

import org.axonframework.messaging.core.Message;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.unitofwork.ProcessingContext;
import org.axonframework.messaging.eventhandling.GenericEventMessage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.axonframework.messaging.tracing.attributes.TracingTestContexts.inFailingContext;
import static org.axonframework.messaging.tracing.attributes.TracingTestContexts.inStartedContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the default methods of the {@link Span}: {@code coverLifecycle}, {@code branch},
 * {@code branchAsync} and {@code branchStream}.
 */
class SpanDefaultsTest {

    private final RecordingSpan span = new RecordingSpan();

    private static Message message(String payload) {
        return new GenericEventMessage(new MessageType("test.event"), payload);
    }

    // --- coverLifecycle ---

    @Test
    void coverLifecycleStartsTheSpanPublishesTheScopeAndClosesItWhenTheContextCompletes() {
        AtomicReference<SpanScope> scope = new AtomicReference<>();
        AtomicReference<SpanScope> published = new AtomicReference<>();

        inStartedContext(context -> {
            scope.set(span.coverLifecycle(context));
            published.set(context.getResource(SpanScope.RESOURCE_KEY));
            assertFalse(scope.get().isClosed());
            return null;
        });

        assertSame(scope.get(), published.get());
        assertEquals(List.of("start", "close"), span.events());
    }

    @Test
    void coverLifecycleRecordsAnErrorOfTheContextBeforeClosingTheScope() {
        IllegalStateException failure = new IllegalStateException("boom");

        inFailingContext(span::coverLifecycle, failure);

        // The unit of work hands the error over wrapped in a CompletionException
        assertEquals(3, span.events().size());
        assertEquals("start", span.events().get(0));
        assertTrue(span.events().get(1).startsWith("exception:"), span.events().toString());
        assertEquals("close", span.events().get(2));
        assertSame(failure, rootCauseOf(span.exceptions().get(0)));
    }

    // --- branch ---

    @Test
    void branchReturnsTheResultAndClosesTheScope() {
        String result = inStartedContext(context -> span.branch(context, scoped -> "result"));

        assertEquals("result", result);
        assertEquals(List.of("start", "close"), span.events());
    }

    @Test
    void branchRunsTheOperationWithinTheScopeAndGivesItAContextWithTheScope() {
        AtomicReference<Integer> depth = new AtomicReference<>();
        AtomicReference<SpanScope> resolved = new AtomicReference<>();

        inStartedContext(context -> span.branch(context, scoped -> {
            depth.set(span.scope().depth());
            resolved.set(SpanScope.fromContext(scoped));
            return null;
        }));

        assertEquals(1, depth.get());
        assertSame(span.scope(), resolved.get());
        assertEquals(1, span.scope().withinCalls());
    }

    @Test
    void branchRecordsAndRethrowsFailuresOfTheOperationAndStillClosesTheScope() {
        IllegalStateException failure = new IllegalStateException("boom");

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> span.branch(null, scoped -> {
                    throw failure;
                }));

        assertSame(failure, thrown);
        assertEquals(List.of("start", "exception:boom", "close"), span.events());
    }

    @Test
    void branchWithoutContextGivesTheOperationNoContext() {
        AtomicReference<ProcessingContext> received = new AtomicReference<>(Mockless.marker());

        span.branch(null, scoped -> {
            received.set(scoped);
            return "done";
        });

        assertNull(received.get());
        assertEquals(List.of("start", "close"), span.events());
    }

    // --- branchAsync ---

    @Test
    void branchAsyncClosesTheScopeOnlyOnceTheFutureCompletes() {
        CompletableFuture<String> pending = new CompletableFuture<>();

        CompletableFuture<String> result = span.branchAsync(null, scoped -> pending);

        assertFalse(span.scope().isClosed());
        pending.complete("done");
        assertEquals("done", result.join());
        assertEquals(List.of("start", "close"), span.events());
    }

    @Test
    void branchAsyncRecordsAFailedFutureAndStillClosesTheScope() {
        IllegalStateException failure = new IllegalStateException("boom");

        CompletableFuture<String> result = span.branchAsync(null, scoped -> CompletableFuture.failedFuture(failure));

        assertTrue(result.isCompletedExceptionally());
        assertEquals(List.of("start", "exception:boom", "close"), span.events());
    }

    @Test
    void branchAsyncRecordsAndRethrowsASynchronousFailureOfTheOperation() {
        IllegalStateException failure = new IllegalStateException("boom");

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> span.branchAsync(null, scoped -> {
                    throw failure;
                }));

        assertSame(failure, thrown);
        assertEquals(List.of("start", "exception:boom", "close"), span.events());
    }

    @Test
    void branchAsyncRejectsAnOperationThatReturnsNull() {
        NullPointerException thrown = assertThrows(NullPointerException.class,
                () -> span.branchAsync(null, scoped -> null));

        assertEquals("The operation returned a null CompletableFuture.", thrown.getMessage());
        assertTrue(span.scope().isClosed());
        assertEquals(1, span.exceptions().size());
    }

    @Test
    void branchAsyncGivesTheOperationAContextWithTheScope() {
        AtomicReference<SpanScope> resolved = new AtomicReference<>();

        inStartedContext(context -> span.branchAsync(context, scoped -> {
            resolved.set(SpanScope.fromContext(scoped));
            return CompletableFuture.completedFuture("done");
        }).join());

        assertSame(span.scope(), resolved.get());
    }

    @Test
    void branchAsyncClosesALeakedScopeWhenTheContextCompletesWithoutRecordingAnException() {
        // The returned future never completes, so only the context level backstop can close the scope
        inStartedContext(context -> {
            span.branchAsync(context, scoped -> new CompletableFuture<String>());
            return null;
        });

        assertEquals(List.of("start", "close"), span.events());
        assertTrue(span.exceptions().isEmpty());
    }

    // --- branchStream ---

    @Test
    void branchStreamPassesTheEntriesThroughAndClosesTheScopeOnceTheStreamCompleted() {
        Message first = message("a");
        Message second = message("b");

        MessageStream<Message> result = span.branchStream(null, scoped -> MessageStream.fromItems(first, second));

        assertEquals(first.identifier(), result.next().orElseThrow().message().identifier());
        assertEquals(second.identifier(), result.next().orElseThrow().message().identifier());
        assertTrue(result.next().isEmpty());
        assertTrue(result.isCompleted());
        assertEquals(List.of("start", "close"), span.events());
        assertTrue(span.scope().withinCalls() >= 1);
    }

    @Test
    void branchStreamRecordsAStreamErrorAndClosesTheScope() {
        IllegalStateException failure = new IllegalStateException("boom");

        MessageStream<Message> result = span.branchStream(null, scoped -> MessageStream.failed(failure));
        result.next();

        assertTrue(result.error().isPresent());
        assertSame(failure, span.exceptions().get(0));
        assertTrue(span.scope().isClosed());
        assertEquals("start", span.events().get(0));
    }

    @Test
    void branchStreamRecordsAndRethrowsFailuresOfTheOperationAndClosesTheScope() {
        IllegalStateException failure = new IllegalStateException("boom");

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> span.<Message>branchStream(null, scoped -> {
                    throw failure;
                }));

        assertSame(failure, thrown);
        assertEquals(List.of("start", "exception:boom", "close"), span.events());
    }

    @Test
    void branchStreamGivesTheOperationAContextWithTheScope() {
        AtomicReference<SpanScope> resolved = new AtomicReference<>();

        inStartedContext(context -> span.<Message>branchStream(context, scoped -> {
            resolved.set(SpanScope.fromContext(scoped));
            return MessageStream.empty().cast();
        }));

        assertSame(span.scope(), resolved.get());
    }

    private static Throwable rootCauseOf(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    /**
     * Gives a non-null starting value so the test proves the operation really received {@code null}.
     */
    private static final class Mockless {

        private static ProcessingContext marker() {
            return (ProcessingContext) java.lang.reflect.Proxy.newProxyInstance(
                    SpanDefaultsTest.class.getClassLoader(), new Class<?>[]{ProcessingContext.class},
                    (proxy, method, args) -> null);
        }
    }
}
