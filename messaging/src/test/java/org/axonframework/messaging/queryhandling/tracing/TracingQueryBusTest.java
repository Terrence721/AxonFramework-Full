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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.axonframework.messaging.queryhandling.tracing;

import org.axonframework.messaging.core.EmptyApplicationContext;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.unitofwork.SimpleUnitOfWorkFactory;
import org.axonframework.messaging.queryhandling.GenericQueryMessage;
import org.axonframework.messaging.queryhandling.GenericQueryResponseMessage;
import org.axonframework.messaging.queryhandling.GenericSubscriptionQueryUpdateMessage;
import org.axonframework.messaging.queryhandling.QueryMessage;
import org.axonframework.messaging.queryhandling.QueryResponseMessage;
import org.axonframework.messaging.queryhandling.SimpleQueryBus;
import org.axonframework.messaging.queryhandling.SubscriptionQueryUpdateMessage;
import org.axonframework.messaging.tracing.LoggingSpanFactory;
import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link TracingQueryBus} delegation, using a real {@link SimpleQueryBus} and the
 * {@link LoggingSpanFactory}.
 */
class TracingQueryBusTest {

    private static final MessageType QUERY_TYPE = new MessageType("test.query");
    private static final MessageType UPDATE_TYPE = new MessageType("test.update");

    private final SimpleQueryBus delegate = new SimpleQueryBus(new SimpleUnitOfWorkFactory(EmptyApplicationContext.INSTANCE));
    private final TracingQueryBus testSubject = new TracingQueryBus(delegate, LoggingSpanFactory.INSTANCE);

    private static QueryMessage query() {
        return new GenericQueryMessage(QUERY_TYPE, "question");
    }

    private static SubscriptionQueryUpdateMessage update(Object payload) {
        return new GenericSubscriptionQueryUpdateMessage(UPDATE_TYPE, payload);
    }

    @Test
    void constructorRejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> new TracingQueryBus(null, LoggingSpanFactory.INSTANCE));
        assertThrows(NullPointerException.class, () -> new TracingQueryBus(delegate, null));
    }

    @Test
    void subscribeReturnsTheTracingBusAndHandlesQueries() {
        assertSame(testSubject, testSubject.subscribe(
                QUERY_TYPE.qualifiedName(),
                (q, c) -> MessageStream.just(new GenericQueryResponseMessage(QUERY_TYPE, "answer"))
        ));

        QueryResponseMessage response = testSubject.query(query(), null)
                                                   .first().asCompletableFuture().join().message();

        assertEquals("answer", response.payload());
    }

    @Test
    void emitUpdateDeliversToMatchingSubscriptions() {
        MessageStream<SubscriptionQueryUpdateMessage> updates = testSubject.subscribeToUpdates(query(), 8);

        testSubject.emitUpdate(q -> true, () -> update("u"), null).join();

        assertEquals("u", updates.first().asCompletableFuture().join().message().payload());
    }

    @Test
    void emitUpdateAndCountReportsTheDelegateCount() {
        testSubject.subscribeToUpdates(query(), 8);
        testSubject.subscribeToUpdates(query(), 8);

        OptionalInt count = testSubject.emitUpdateAndCount(q -> true, () -> update("u"), null).join();

        assertEquals(OptionalInt.of(2), count);
    }

    @Test
    void completeSubscriptionsAndCountReportsTheDelegateCount() {
        MessageStream<SubscriptionQueryUpdateMessage> updates = testSubject.subscribeToUpdates(query(), 8);

        OptionalInt count = testSubject.completeSubscriptionsAndCount(q -> true, null).join();

        assertEquals(OptionalInt.of(1), count);
        assertTrue(updates.next().isEmpty());
        assertTrue(updates.isCompleted());
    }

    @Test
    void completeSubscriptionsCompletesMatchingSubscriptions() {
        MessageStream<SubscriptionQueryUpdateMessage> updates = testSubject.subscribeToUpdates(query(), 8);

        testSubject.completeSubscriptions(q -> true, null).join();

        assertTrue(updates.next().isEmpty());
        assertTrue(updates.isCompleted());
    }

    @Test
    void completeSubscriptionsExceptionallyAndCountReportsTheDelegateCount() {
        MessageStream<SubscriptionQueryUpdateMessage> updates = testSubject.subscribeToUpdates(query(), 8);
        IllegalStateException cause = new IllegalStateException("stop");

        OptionalInt count = testSubject.completeSubscriptionsExceptionallyAndCount(q -> true, cause, null).join();

        assertEquals(OptionalInt.of(1), count);
        assertTrue(updates.next().isEmpty());
        assertSame(cause, updates.error().orElseThrow());
    }

    @Test
    void completeSubscriptionsExceptionallyFailsMatchingSubscriptions() {
        MessageStream<SubscriptionQueryUpdateMessage> updates = testSubject.subscribeToUpdates(query(), 8);
        IllegalStateException cause = new IllegalStateException("stop");

        testSubject.completeSubscriptionsExceptionally(q -> true, cause, null).join();

        assertTrue(updates.next().isEmpty());
        assertSame(cause, updates.error().orElseThrow());
    }
}
