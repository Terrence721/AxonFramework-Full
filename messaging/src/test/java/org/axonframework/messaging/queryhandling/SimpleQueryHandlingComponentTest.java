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

import org.axonframework.common.infra.ComponentDescriptor;
import org.axonframework.messaging.core.EmptyApplicationContext;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.QualifiedName;
import org.axonframework.messaging.core.unitofwork.ProcessingContext;
import org.axonframework.messaging.core.unitofwork.SimpleUnitOfWorkFactory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link SimpleQueryHandlingComponent}.
 */
class SimpleQueryHandlingComponentTest {

    private static final MessageType QUERY_TYPE = new MessageType("test.query");
    private static final QualifiedName QUERY_NAME = QUERY_TYPE.qualifiedName();
    private static final MessageType OTHER_TYPE = new MessageType("test.other");

    private final SimpleQueryHandlingComponent testSubject = SimpleQueryHandlingComponent.create("test");
    private final QueryMessage query = new GenericQueryMessage(QUERY_TYPE, "payload");

    private static QueryResponseMessage response(String value) {
        return new GenericQueryResponseMessage(QUERY_TYPE, value);
    }

    private <T> T inContext(Function<ProcessingContext, T> action) {
        return new SimpleUnitOfWorkFactory(EmptyApplicationContext.INSTANCE)
                .create()
                .executeWithResult(context -> CompletableFuture.completedFuture(action.apply(context)))
                .join();
    }

    @Test
    void createRejectsNullAndEmptyNames() {
        assertThrows(IllegalArgumentException.class, () -> SimpleQueryHandlingComponent.create(null));
        assertThrows(IllegalArgumentException.class, () -> SimpleQueryHandlingComponent.create(""));
    }

    @Test
    void handlesQueryWithSubscribedHandler() {
        QueryResponseMessage expected = response("handler");
        testSubject.subscribe(QUERY_NAME, (message, context) -> MessageStream.fromItems(expected));

        MessageStream<QueryResponseMessage> result = inContext(context -> testSubject.handle(query, context));

        assertSame(expected, result.next().orElseThrow().message());
    }

    @Test
    void queryWithoutHandlerFailsWithNoHandlerForQueryException() {
        MessageStream<QueryResponseMessage> result = inContext(context -> testSubject.handle(query, context));

        assertTrue(result.isCompleted());
        assertInstanceOf(NoHandlerForQueryException.class, result.error().orElseThrow());
    }

    @Test
    void handlerThrowingExceptionYieldsFailedStream() {
        IllegalStateException failure = new IllegalStateException("boom");
        testSubject.subscribe(QUERY_NAME, (message, context) -> {
            throw failure;
        });

        MessageStream<QueryResponseMessage> result = inContext(context -> testSubject.handle(query, context));

        assertSame(failure, result.error().orElseThrow());
    }

    @Test
    void subscribingDifferentHandlerForSameNameIsRejected() {
        testSubject.subscribe(QUERY_NAME, (message, context) -> MessageStream.empty().cast());

        assertThrows(DuplicateQueryHandlerSubscriptionException.class,
                     () -> testSubject.subscribe(QUERY_NAME, (message, context) -> MessageStream.empty().cast()));
    }

    @Test
    void subscribingSameHandlerTwiceIsIdempotent() {
        QueryHandler handler = (message, context) -> MessageStream.empty().cast();
        testSubject.subscribe(QUERY_NAME, handler);

        testSubject.subscribe(QUERY_NAME, handler);

        assertEquals(Set.of(QUERY_NAME), testSubject.supportedQueries());
    }

    @Test
    void subscribeReturnsTheComponentForChaining() {
        assertSame(testSubject, testSubject.subscribe(QUERY_NAME, (message, context) -> MessageStream.empty().cast()));
    }

    @Test
    void componentSubscribedAsHandlerIsRegisteredAsSubComponent() {
        QueryResponseMessage expected = response("sub");
        SimpleQueryHandlingComponent sub = SimpleQueryHandlingComponent.create("sub")
                                                                       .subscribe(QUERY_NAME, (m, c) -> MessageStream.fromItems(expected));

        testSubject.subscribe(OTHER_TYPE.qualifiedName(), sub);

        assertEquals(Set.of(QUERY_NAME), testSubject.supportedQueries());
        MessageStream<QueryResponseMessage> result = inContext(context -> testSubject.handle(query, context));
        assertSame(expected, result.next().orElseThrow().message());
    }

    @Test
    void subComponentIsPreferredOverRegisteredHandler() {
        AtomicBoolean handlerInvoked = new AtomicBoolean();
        QueryResponseMessage fromSub = response("sub");
        testSubject.subscribe(QUERY_NAME, (message, context) -> {
            handlerInvoked.set(true);
            return MessageStream.empty().cast();
        });
        testSubject.subscribe(SimpleQueryHandlingComponent.create("sub")
                                                          .subscribe(QUERY_NAME,
                                                                     (m, c) -> MessageStream.fromItems(fromSub)));

        MessageStream<QueryResponseMessage> result = inContext(context -> testSubject.handle(query, context));

        assertSame(fromSub, result.next().orElseThrow().message());
        assertFalse(handlerInvoked.get());
    }

    @Test
    void subComponentThrowingExceptionYieldsFailedStream() {
        IllegalStateException failure = new IllegalStateException("sub boom");
        testSubject.subscribe(new QueryHandlingComponent() {
            @Override
            public MessageStream<QueryResponseMessage> handle(QueryMessage query, ProcessingContext context) {
                throw failure;
            }

            @Override
            public Set<QualifiedName> supportedQueries() {
                return Set.of(QUERY_NAME);
            }

            @Override
            public void describeTo(ComponentDescriptor descriptor) {
            }
        });

        MessageStream<QueryResponseMessage> result = inContext(context -> testSubject.handle(query, context));

        assertSame(failure, result.error().orElseThrow());
    }

    @Test
    void supportedQueriesCombinesHandlersAndSubComponents() {
        QualifiedName subName = OTHER_TYPE.qualifiedName();
        testSubject.subscribe(QUERY_NAME, (message, context) -> MessageStream.empty().cast());
        testSubject.subscribe(SimpleQueryHandlingComponent.create("sub")
                                                          .subscribe(subName,
                                                                     (m, c) -> MessageStream.empty().cast()));

        assertEquals(Set.of(QUERY_NAME, subName), testSubject.supportedQueries());
    }

    @Test
    void describesNameHandlersAndSubComponents() {
        testSubject.subscribe(QUERY_NAME, (message, context) -> MessageStream.empty().cast());
        Map<String, Object> described = new HashMap<>();

        testSubject.describeTo((ComponentDescriptor) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{ComponentDescriptor.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("describeProperty")) {
                        described.put((String) args[0], args[1]);
                    }
                    return null;
                }));

        assertEquals("test", described.get("name"));
        assertTrue(described.containsKey("queryHandlers"));
        assertTrue(described.containsKey("subComponents"));
    }
}
