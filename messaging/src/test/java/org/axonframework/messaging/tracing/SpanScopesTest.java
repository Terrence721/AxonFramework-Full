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
import org.axonframework.messaging.eventhandling.GenericEventMessage;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.axonframework.messaging.tracing.attributes.TracingTestContexts.inStartedContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the context handling of the {@link SpanScope}, the {@link BranchSpanScope} and the
 * {@link SpanScopedMessageStream}.
 */
class SpanScopesTest {

    private static RecordingSpan.Scope newScope() {
        return (RecordingSpan.Scope) new RecordingSpan().start();
    }

    private static Message message(String payload) {
        return new GenericEventMessage(new MessageType("test.event"), payload);
    }

    // --- SpanScope static helpers ---

    @Test
    void aScopeAddedToAContextCanBeResolvedFromThatContextOnly() {
        RecordingSpan.Scope scope = newScope();

        inStartedContext(context -> {
            var branched = SpanScope.addToContext(context, scope);

            assertSame(scope, SpanScope.fromContext(branched));
            assertNull(SpanScope.fromContext(context));
            return null;
        });
    }

    @Test
    void aClosedScopeIsNoLongerResolvedFromTheContext() {
        RecordingSpan.Scope scope = newScope();

        inStartedContext(context -> {
            var branched = SpanScope.addToContext(context, scope);
            scope.close();

            assertNull(SpanScope.fromContext(branched));
            return null;
        });
    }

    @Test
    void resolvingFallsBackToTheOpenParentWhenTheNestedScopeIsClosed() {
        RecordingSpan.Scope parent = newScope();
        RecordingSpan.Scope child = newScope();

        inStartedContext(context -> {
            var nested = SpanScope.addToContext(SpanScope.addToContext(context, parent), child);
            assertSame(child, SpanScope.fromContext(nested));

            child.close();
            assertSame(parent, SpanScope.fromContext(nested));

            parent.close();
            assertNull(SpanScope.fromContext(nested));
            return null;
        });
    }

    @Test
    void withinRunnableRunsTheRunnableThroughTheSupplierVariant() {
        RecordingSpan.Scope scope = newScope();
        AtomicInteger runs = new AtomicInteger();

        scope.within((Runnable) runs::incrementAndGet);

        assertEquals(1, runs.get());
        assertEquals(1, scope.withinCalls());
    }

    // --- BranchSpanScope ---

    @Test
    void branchScopeRejectsANullDelegate() {
        NullPointerException exception =
                assertThrows(NullPointerException.class, () -> new BranchSpanScope(null, null));

        assertEquals("delegate may not be null", exception.getMessage());
    }

    @Test
    void branchScopeDelegatesEverythingToTheScopeItWraps() {
        RecordingSpan.Scope delegate = newScope();
        BranchSpanScope testSubject = new BranchSpanScope(delegate, null);

        assertSame(delegate.span(), testSubject.span());
        assertFalse(testSubject.isClosed());
        assertEquals("result", testSubject.within(() -> "result"));
        assertEquals(1, delegate.withinCalls());

        testSubject.close();
        assertTrue(delegate.isClosed());
        assertTrue(testSubject.isClosed());
    }

    @Test
    void resolvingNothingGivesNothing() {
        assertNull(BranchSpanScope.resolve(null));
    }

    @Test
    void resolvingAPlainScopeGivesThatScopeWhileItIsOpen() {
        RecordingSpan.Scope scope = newScope();

        assertSame(scope, BranchSpanScope.resolve(scope));

        scope.close();
        assertNull(BranchSpanScope.resolve(scope));
    }

    @Test
    void resolvingABranchGivesItsOpenDelegate() {
        RecordingSpan.Scope delegate = newScope();

        assertSame(delegate, BranchSpanScope.resolve(new BranchSpanScope(delegate, null)));
    }

    @Test
    void resolvingABranchWithAClosedDelegateWalksUpToTheOpenParent() {
        RecordingSpan.Scope parent = newScope();
        RecordingSpan.Scope closedChild = newScope();
        closedChild.close();

        assertSame(parent, BranchSpanScope.resolve(new BranchSpanScope(closedChild, parent)));
    }

    @Test
    void resolvingABranchChainWithOnlyClosedScopesGivesNothing() {
        RecordingSpan.Scope parent = newScope();
        RecordingSpan.Scope child = newScope();
        parent.close();
        child.close();

        assertNull(BranchSpanScope.resolve(new BranchSpanScope(child, new BranchSpanScope(parent, null))));
    }

    // --- SpanScopedMessageStream ---

    @Test
    void scopedStreamRejectsNullArguments() {
        RecordingSpan.Scope scope = newScope();

        assertEquals("The delegate may not be null.", assertThrows(NullPointerException.class,
                () -> new SpanScopedMessageStream<Message>(null, scope)).getMessage());
        assertEquals("The scope may not be null.", assertThrows(NullPointerException.class,
                () -> new SpanScopedMessageStream<>(MessageStream.<Message>empty().cast(), null)).getMessage());
    }

    @Test
    void scopedStreamPassesTheEntriesOfItsDelegateThrough() {
        Message first = message("a");
        Message second = message("b");
        SpanScopedMessageStream<Message> testSubject =
                new SpanScopedMessageStream<>(MessageStream.fromItems(first, second), newScope());

        assertEquals(first.identifier(), testSubject.next().orElseThrow().message().identifier());
        assertEquals(second.identifier(), testSubject.next().orElseThrow().message().identifier());
        assertTrue(testSubject.next().isEmpty());
        assertTrue(testSubject.isCompleted());
    }

    @Test
    void scopedStreamReEntersTheScopeForEveryPull() {
        RecordingSpan.Scope scope = newScope();
        SpanScopedMessageStream<Message> testSubject =
                new SpanScopedMessageStream<>(MessageStream.fromItems(message("a"), message("b")), scope);
        int before = scope.withinCalls();

        testSubject.next();
        int afterFirst = scope.withinCalls();
        testSubject.next();

        assertTrue(afterFirst > before, "The first pull did not enter the scope");
        assertTrue(scope.withinCalls() > afterFirst, "The second pull did not enter the scope");
    }

    @Test
    void scopedStreamOfAnEmptyDelegateIsCompletedRightAway() {
        SpanScopedMessageStream<Message> testSubject =
                new SpanScopedMessageStream<>(MessageStream.<Message>empty().cast(), newScope());

        assertTrue(testSubject.isCompleted());
        assertTrue(testSubject.error().isEmpty());
        assertTrue(testSubject.next().isEmpty());
    }

    @Test
    void scopedStreamOfAFailedDelegateReportsThatFailure() {
        IllegalStateException failure = new IllegalStateException("boom");
        SpanScopedMessageStream<Message> testSubject =
                new SpanScopedMessageStream<>(MessageStream.<Message>failed(failure).cast(), newScope());

        assertTrue(testSubject.isCompleted());
        assertSame(failure, testSubject.error().orElseThrow());
    }
}
