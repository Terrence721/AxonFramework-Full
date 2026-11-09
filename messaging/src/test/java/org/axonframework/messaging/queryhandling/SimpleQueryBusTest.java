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
package org.axonframework.messaging.queryhandling;

import org.axonframework.messaging.core.EmptyApplicationContext;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.unitofwork.SimpleUnitOfWorkFactory;
import org.junit.jupiter.api.Test;

import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleQueryBusTest {

    private static final MessageType QUERY_TYPE = new MessageType("test.query");

    private final SimpleQueryBus testSubject =
            new SimpleQueryBus(new SimpleUnitOfWorkFactory(EmptyApplicationContext.INSTANCE));

    @Test
    void queryDispatchesToHandlerRegisteredForQueryName() {
        QueryMessage query = new GenericQueryMessage(QUERY_TYPE, "payload");
        QueryResponseMessage response = new GenericQueryResponseMessage(QUERY_TYPE, "response");
        testSubject.subscribe(QUERY_TYPE.qualifiedName(),
                              (message, context) -> MessageStream.fromItems(response));

        MessageStream<QueryResponseMessage> result = testSubject.query(query, null);

        assertTrue(result.hasNextAvailable());
        assertTrue(result.next().map(entry -> entry.message() == response).orElse(false));
    }

    @Test
    void queryWithoutHandlerReturnsFailedStream() {
        MessageStream<QueryResponseMessage> result =
                testSubject.query(new GenericQueryMessage(QUERY_TYPE, "payload"), null);

        assertTrue(result.isCompleted());
        assertInstanceOf(NoHandlerForQueryException.class, result.error().orElseThrow());
    }

    @Test
    void duplicateUpdateSubscriptionReturnsFailedStreamAndKeepsOriginalSubscriptionOpen() {
        QueryMessage query = new GenericQueryMessage(QUERY_TYPE, "payload");

        MessageStream<SubscriptionQueryUpdateMessage> original = testSubject.subscribeToUpdates(query, 1);
        MessageStream<SubscriptionQueryUpdateMessage> duplicate = testSubject.subscribeToUpdates(query, 1);

        assertTrue(duplicate.isCompleted());
        assertInstanceOf(SubscriptionQueryAlreadyRegisteredException.class, duplicate.error().orElseThrow());
        assertFalse(original.isCompleted());
    }

    @Test
    void duplicateSubscriptionQueryDoesNotInvokeQueryHandlerWhenAlreadyRegistered() {
        QueryMessage query = new GenericQueryMessage(QUERY_TYPE, "payload");
        AtomicBoolean handlerInvoked = new AtomicBoolean();
        testSubject.subscribe(QUERY_TYPE.qualifiedName(), (message, context) -> {
            handlerInvoked.set(true);
            return MessageStream.empty();
        });
        testSubject.subscribeToUpdates(query, 1);

        MessageStream<QueryResponseMessage> duplicate = testSubject.subscriptionQuery(query, null, 1);

        assertTrue(duplicate.isCompleted());
        assertInstanceOf(SubscriptionQueryAlreadyRegisteredException.class, duplicate.error().orElseThrow());
        assertFalse(handlerInvoked.get());
    }

    @Test
    void emitUpdateOnlyDeliversToMatchingQueriesAndCreatesUpdateOnce() {
        QueryMessage matchingQuery = new GenericQueryMessage(QUERY_TYPE, "matching");
        QueryMessage otherQuery = new GenericQueryMessage(QUERY_TYPE, "other");
        MessageStream<SubscriptionQueryUpdateMessage> matchingUpdates = testSubject.subscribeToUpdates(matchingQuery, 1);
        MessageStream<SubscriptionQueryUpdateMessage> otherUpdates = testSubject.subscribeToUpdates(otherQuery, 1);
        SubscriptionQueryUpdateMessage update =
                new GenericSubscriptionQueryUpdateMessage(new MessageType("test.update"), "update");
        AtomicInteger supplierCalls = new AtomicInteger();

        OptionalInt delivered = testSubject.emitUpdateAndCount(
                query -> query.payload().equals("matching"),
                () -> {
                    supplierCalls.incrementAndGet();
                    return update;
                },
                null
        ).join();

        assertEquals(OptionalInt.of(1), delivered);
        assertEquals(1, supplierCalls.get());
        assertSame(update, matchingUpdates.next().orElseThrow().message());
        assertFalse(otherUpdates.hasNextAvailable());
    }

    @Test
    void completingSubscriptionsOnlyCompletesMatchingUpdateStreams() {
        QueryMessage matchingQuery = new GenericQueryMessage(QUERY_TYPE, "matching");
        QueryMessage otherQuery = new GenericQueryMessage(QUERY_TYPE, "other");
        MessageStream<SubscriptionQueryUpdateMessage> matchingUpdates = testSubject.subscribeToUpdates(matchingQuery, 1);
        MessageStream<SubscriptionQueryUpdateMessage> otherUpdates = testSubject.subscribeToUpdates(otherQuery, 1);

        OptionalInt completed = testSubject.completeSubscriptionsAndCount(
                query -> query.payload().equals("matching"),
                null
        ).join();

        assertEquals(OptionalInt.of(1), completed);
        assertTrue(matchingUpdates.next().isEmpty());
        assertTrue(matchingUpdates.isCompleted());
        assertFalse(otherUpdates.isCompleted());
    }

    @Test
    void completingSubscriptionsExceptionallyPropagatesCauseToMatchingUpdateStream() {
        QueryMessage query = new GenericQueryMessage(QUERY_TYPE, "payload");
        MessageStream<SubscriptionQueryUpdateMessage> updates = testSubject.subscribeToUpdates(query, 1);
        RuntimeException cause = new RuntimeException("failure");

        OptionalInt completed = testSubject.completeSubscriptionsExceptionallyAndCount(
                ignored -> true,
                cause,
                null
        ).join();

        assertEquals(OptionalInt.of(1), completed);
        assertTrue(updates.next().isEmpty());
        assertSame(cause, updates.error().orElseThrow());
    }

    @Test
    void emitUpdateIsDeferredUntilProcessingContextCommits() {
        QueryMessage query = new GenericQueryMessage(QUERY_TYPE, "payload");
        MessageStream<SubscriptionQueryUpdateMessage> updates = testSubject.subscribeToUpdates(query, 1);
        SubscriptionQueryUpdateMessage update =
                new GenericSubscriptionQueryUpdateMessage(new MessageType("test.update"), "update");
        AtomicReference<OptionalInt> result = new AtomicReference<>();

        new SimpleUnitOfWorkFactory(EmptyApplicationContext.INSTANCE)
                .create()
                .executeWithResult(context -> {
                    result.set(testSubject.emitUpdateAndCount(ignored -> true, () -> update, context).join());
                    assertFalse(updates.hasNextAvailable());
                    return CompletableFuture.completedFuture(null);
                })
                .join();

        assertEquals(OptionalInt.of(1), result.get());
        assertSame(update, updates.next().orElseThrow().message());
    }

    @Test
    void overflowingUpdateBufferFailsAndRemovesTheSubscription() {
        QueryMessage query = new GenericQueryMessage(QUERY_TYPE, "payload");
        MessageStream<SubscriptionQueryUpdateMessage> updates = testSubject.subscribeToUpdates(query, 1);
        SubscriptionQueryUpdateMessage firstUpdate =
                new GenericSubscriptionQueryUpdateMessage(new MessageType("test.update"), "first");
        SubscriptionQueryUpdateMessage secondUpdate =
                new GenericSubscriptionQueryUpdateMessage(new MessageType("test.update"), "second");

        assertEquals(OptionalInt.of(1), testSubject.emitUpdateAndCount(
                ignored -> true, () -> firstUpdate, null
        ).join());
        assertEquals(OptionalInt.of(0), testSubject.emitUpdateAndCount(
                ignored -> true, () -> secondUpdate, null
        ).join());
        assertSame(firstUpdate, updates.next().orElseThrow().message());
        assertTrue(updates.next().isEmpty());
        assertInstanceOf(QueryExecutionException.class, updates.error().orElseThrow());
    }
}
