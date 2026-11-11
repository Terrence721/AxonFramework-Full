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
package org.axonframework.messaging.queryhandling.interception;

import org.axonframework.messaging.core.EmptyApplicationContext;
import org.axonframework.messaging.core.MessageDispatchInterceptor;
import org.axonframework.messaging.core.MessageHandlerInterceptor;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.unitofwork.SimpleUnitOfWorkFactory;
import org.axonframework.messaging.queryhandling.GenericQueryMessage;
import org.axonframework.messaging.queryhandling.GenericQueryResponseMessage;
import org.axonframework.messaging.queryhandling.GenericSubscriptionQueryUpdateMessage;
import org.axonframework.messaging.queryhandling.QueryBus;
import org.axonframework.messaging.queryhandling.QueryMessage;
import org.axonframework.messaging.queryhandling.QueryResponseMessage;
import org.axonframework.messaging.queryhandling.SimpleQueryBus;
import org.axonframework.messaging.queryhandling.SubscriptionQueryUpdateMessage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link InterceptingQueryBus}, {@link InterceptingQueryHandlingComponent} and
 * {@link QueryMessageHandlerInterceptorChain}, using a real {@link SimpleQueryBus} as delegate.
 */
class InterceptingQueryBusTest {

    private static final MessageType QUERY_TYPE = new MessageType("test.query");
    private static final MessageType UPDATE_TYPE = new MessageType("test.update");

    private final SimpleQueryBus delegate = new SimpleQueryBus(new SimpleUnitOfWorkFactory(EmptyApplicationContext.INSTANCE));
    private final List<String> calls = new ArrayList<>();

    private InterceptingQueryBus bus(
            List<MessageHandlerInterceptor<? super QueryMessage>> handlerInterceptors,
            List<MessageDispatchInterceptor<? super QueryMessage>> dispatchInterceptors,
            List<MessageDispatchInterceptor<? super SubscriptionQueryUpdateMessage>> updateInterceptors
    ) {
        return new InterceptingQueryBus(delegate, handlerInterceptors, dispatchInterceptors, updateInterceptors);
    }

    private InterceptingQueryBus plainBus() {
        return bus(List.of(), List.of(), List.of());
    }

    private MessageHandlerInterceptor<QueryMessage> handlerInterceptor(String name) {
        return (query, context, chain) -> {
            calls.add(name);
            return chain.proceed(query, context);
        };
    }

    private MessageDispatchInterceptor<QueryMessage> dispatchInterceptor(String name) {
        return (query, context, chain) -> {
            calls.add(name);
            return chain.proceed(query, context);
        };
    }

    private static QueryMessage query() {
        return new GenericQueryMessage(QUERY_TYPE, "question");
    }

    private static SubscriptionQueryUpdateMessage update(Object payload) {
        return new GenericSubscriptionQueryUpdateMessage(UPDATE_TYPE, payload);
    }

    @Test
    void constructorRejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> new InterceptingQueryBus(null, List.of(), List.of(), List.of()));
        assertThrows(NullPointerException.class, () -> new InterceptingQueryBus(delegate, null, List.of(), List.of()));
        assertThrows(NullPointerException.class, () -> new InterceptingQueryBus(delegate, List.of(), null, List.of()));
        assertThrows(NullPointerException.class, () -> new InterceptingQueryBus(delegate, List.of(), List.of(), null));
    }

    @Test
    void subscribeReturnsTheInterceptingBus() {
        InterceptingQueryBus bus = plainBus();

        assertSame(bus, bus.subscribe(QUERY_TYPE.qualifiedName(), (q, c) -> MessageStream.empty().cast()));
    }

    @Test
    void handlerWithoutInterceptorsIsSubscribedAsIs() {
        InterceptingQueryBus bus = plainBus();
        bus.subscribe(QUERY_TYPE.qualifiedName(),
                      (q, c) -> MessageStream.just(new GenericQueryResponseMessage(QUERY_TYPE, "answer")));

        QueryResponseMessage response = bus.query(query(), null).first().asCompletableFuture().join().message();

        assertEquals("answer", response.payload());
    }

    @Test
    void handlerInterceptorsAreInvokedInRegistrationOrderAroundTheHandler() {
        InterceptingQueryBus bus = bus(List.of(handlerInterceptor("first"), handlerInterceptor("second")),
                                       List.of(),
                                       List.of());
        bus.subscribe(QUERY_TYPE.qualifiedName(), (q, c) -> {
            calls.add("handler");
            return MessageStream.just(new GenericQueryResponseMessage(QUERY_TYPE, "answer"));
        });

        QueryResponseMessage response = bus.query(query(), null).first().asCompletableFuture().join().message();

        assertEquals("answer", response.payload());
        assertEquals(List.of("first", "second", "handler"), calls);
    }

    @Test
    void handlerInterceptorCanShortCircuit() {
        MessageHandlerInterceptor<QueryMessage> shortCircuit = (q, c, chain) -> {
            calls.add("intercepted");
            return MessageStream.just(new GenericQueryResponseMessage(QUERY_TYPE, "short-circuit"));
        };
        InterceptingQueryBus bus = bus(List.of(shortCircuit), List.of(), List.of());
        bus.subscribe(QUERY_TYPE.qualifiedName(), (q, c) -> {
            calls.add("handler");
            return MessageStream.just(new GenericQueryResponseMessage(QUERY_TYPE, "answer"));
        });

        QueryResponseMessage response = bus.query(query(), null).first().asCompletableFuture().join().message();

        assertEquals("short-circuit", response.payload());
        assertEquals(List.of("intercepted"), calls);
    }

    @Test
    void exceptionThrownByAnInterceptorFailsTheStream() {
        MessageHandlerInterceptor<QueryMessage> failing = (q, c, chain) -> {
            throw new IllegalStateException("boom");
        };
        InterceptingQueryBus bus = bus(List.of(failing), List.of(), List.of());
        bus.subscribe(QUERY_TYPE.qualifiedName(), (q, c) -> MessageStream.empty().cast());

        MessageStream<QueryResponseMessage> result = bus.query(query(), null);

        assertTrue(result.error().isPresent()
                           || result.first().asCompletableFuture().isCompletedExceptionally());
    }

    @Test
    void dispatchInterceptorsAreInvokedBeforeTheDelegateAndInOrder() {
        InterceptingQueryBus bus = bus(List.of(),
                                       List.of(dispatchInterceptor("d1"), dispatchInterceptor("d2")),
                                       List.of());
        bus.subscribe(QUERY_TYPE.qualifiedName(), (q, c) -> {
            calls.add("handler");
            return MessageStream.just(new GenericQueryResponseMessage(QUERY_TYPE, "answer"));
        });

        bus.query(query(), null).first().asCompletableFuture().join();

        assertEquals(List.of("d1", "d2", "handler"), calls);
    }

    @Test
    void dispatchInterceptorsApplyToSubscriptionQueries() {
        InterceptingQueryBus bus = bus(List.of(), List.of(dispatchInterceptor("d1")), List.of());
        bus.subscribe(QUERY_TYPE.qualifiedName(),
                      (q, c) -> MessageStream.just(new GenericQueryResponseMessage(QUERY_TYPE, "initial")));

        QueryResponseMessage response = bus.subscriptionQuery(query(), null, 8)
                                           .first().asCompletableFuture().join().message();

        assertEquals("initial", response.payload());
        assertEquals(List.of("d1"), calls);
    }

    @Test
    void dispatchInterceptorsApplyToSubscribeToUpdates() {
        InterceptingQueryBus bus = bus(List.of(), List.of(dispatchInterceptor("d1")), List.of());

        MessageStream<SubscriptionQueryUpdateMessage> updates = bus.subscribeToUpdates(query(), 8);

        assertEquals(List.of("d1"), calls);
        assertEquals(OptionalInt.of(1),
                     delegate.emitUpdateAndCount(q -> true, () -> update("u"), null).join());
        assertEquals("u", updates.first().asCompletableFuture().join().message().payload());
    }

    @Test
    void emitUpdateWithoutUpdateInterceptorsDeliversTheUpdate() {
        InterceptingQueryBus bus = plainBus();
        MessageStream<SubscriptionQueryUpdateMessage> updates = bus.subscribeToUpdates(query(), 8);

        bus.emitUpdate(q -> true, () -> update("u"), null).join();

        assertEquals("u", updates.first().asCompletableFuture().join().message().payload());
    }

    @Test
    void updateInterceptorsAreAppliedToEmittedUpdatesExactlyOnce() {
        AtomicInteger invocations = new AtomicInteger();
        MessageDispatchInterceptor<SubscriptionQueryUpdateMessage> interceptor = (update, context, chain) -> {
            invocations.incrementAndGet();
            return chain.proceed(update.withMetadata(update.metadata().and("intercepted", "yes")), context);
        };
        InterceptingQueryBus bus = bus(List.of(), List.of(), List.of(interceptor));
        MessageStream<SubscriptionQueryUpdateMessage> first = bus.subscribeToUpdates(query(), 8);
        MessageStream<SubscriptionQueryUpdateMessage> second = bus.subscribeToUpdates(query(), 8);

        bus.emitUpdate(q -> true, () -> update("u"), null).join();

        assertEquals(1, invocations.get());
        assertEquals("yes", first.first().asCompletableFuture().join().message().metadata().get("intercepted"));
        assertEquals("yes", second.first().asCompletableFuture().join().message().metadata().get("intercepted"));
    }

    @Test
    void failingUpdateSupplierFailsTheFutureWhenUpdateInterceptorsArePresent() {
        MessageDispatchInterceptor<SubscriptionQueryUpdateMessage> interceptor =
                (update, context, chain) -> chain.proceed(update, context);
        InterceptingQueryBus bus = bus(List.of(), List.of(), List.of(interceptor));

        CompletableFuture<Void> result = bus.emitUpdate(q -> true, () -> {
            throw new IllegalStateException("no update");
        }, null);

        assertTrue(result.isCompletedExceptionally());
    }

    @Test
    void emitUpdateAndCountReportsTheDelegateCountWithoutInterceptors() {
        InterceptingQueryBus bus = plainBus();
        bus.subscribeToUpdates(query(), 8);
        bus.subscribeToUpdates(new GenericQueryMessage(new MessageType("other.query"), "x"), 8);

        OptionalInt count = bus.emitUpdateAndCount(q -> true, () -> update("u"), null).join();

        assertEquals(OptionalInt.of(2), count);
    }

    @Test
    void emitUpdateAndCountReportsTheDelegateCountWithUpdateInterceptors() {
        MessageDispatchInterceptor<SubscriptionQueryUpdateMessage> interceptor =
                (update, context, chain) -> chain.proceed(update, context);
        InterceptingQueryBus bus = bus(List.of(), List.of(), List.of(interceptor));
        bus.subscribeToUpdates(query(), 8);

        OptionalInt count = bus.emitUpdateAndCount(q -> true, () -> update("u"), null).join();

        assertEquals(OptionalInt.of(1), count);
    }

    @Test
    void completeSubscriptionsAndCountReportsTheDelegateCount() {
        InterceptingQueryBus bus = plainBus();
        MessageStream<SubscriptionQueryUpdateMessage> updates = bus.subscribeToUpdates(query(), 8);

        OptionalInt count = bus.completeSubscriptionsAndCount(q -> true, null).join();

        assertEquals(OptionalInt.of(1), count);
        assertTrue(updates.next().isEmpty());
        assertTrue(updates.isCompleted());
    }

    @Test
    void completeSubscriptionsExceptionallyAndCountReportsTheDelegateCount() {
        InterceptingQueryBus bus = plainBus();
        MessageStream<SubscriptionQueryUpdateMessage> updates = bus.subscribeToUpdates(query(), 8);
        IllegalStateException cause = new IllegalStateException("stop");

        OptionalInt count = bus.completeSubscriptionsExceptionallyAndCount(q -> true, cause, null).join();

        assertEquals(OptionalInt.of(1), count);
        assertTrue(updates.next().isEmpty());
        assertSame(cause, updates.error().orElseThrow());
    }

    @Test
    void completeMethodsDelegate() {
        InterceptingQueryBus bus = plainBus();
        MessageStream<SubscriptionQueryUpdateMessage> completed = bus.subscribeToUpdates(query(), 8);

        bus.completeSubscriptions(q -> true, null).join();

        assertTrue(completed.next().isEmpty());
        assertTrue(completed.isCompleted());
    }

    // --- InterceptingQueryHandlingComponent / chain ---

    @Test
    void handlingComponentAppliesInterceptorsInOrderAndExposesSupportedQueries() {
        org.axonframework.messaging.queryhandling.SimpleQueryHandlingComponent inner =
                org.axonframework.messaging.queryhandling.SimpleQueryHandlingComponent.create("inner");
        inner.subscribe(QUERY_TYPE.qualifiedName(), (q, c) -> {
            calls.add("handler");
            return MessageStream.just(new GenericQueryResponseMessage(QUERY_TYPE, "answer"));
        });
        InterceptingQueryHandlingComponent component = new InterceptingQueryHandlingComponent(
                List.of(handlerInterceptor("first"), handlerInterceptor("second")), inner
        );

        QueryResponseMessage response = new SimpleUnitOfWorkFactory(EmptyApplicationContext.INSTANCE)
                .create()
                .executeWithResult(context -> CompletableFuture.completedFuture(component.handle(query(), context)))
                .join().first().asCompletableFuture().join().message();

        assertEquals("answer", response.payload());
        assertEquals(List.of("first", "second", "handler"), calls);
        assertEquals(inner.supportedQueries(), component.supportedQueries());
    }

    @Test
    void handlingComponentRejectsNullArguments() {
        org.axonframework.messaging.queryhandling.SimpleQueryHandlingComponent inner =
                org.axonframework.messaging.queryhandling.SimpleQueryHandlingComponent.create("inner");

        assertThrows(NullPointerException.class, () -> new InterceptingQueryHandlingComponent(null, inner));
        assertThrows(NullPointerException.class, () -> new InterceptingQueryHandlingComponent(List.of(), null));
    }

    @Test
    void chainConvertsThrownExceptionsIntoAFailedStream() {
        MessageHandlerInterceptor<QueryMessage> failing = (q, c, chain) -> {
            throw new IllegalStateException("boom");
        };
        QueryMessageHandlerInterceptorChain chain = new QueryMessageHandlerInterceptorChain(
                List.of(failing), (q, c) -> MessageStream.empty().cast()
        );

        MessageStream<?> result = chain.proceed(query(), null);

        assertInstanceOf(IllegalStateException.class, result.error().orElseThrow());
    }

    @Test
    void chainRejectsANullHandler() {
        assertThrows(NullPointerException.class, () -> new QueryMessageHandlerInterceptorChain(List.of(), null));
    }

    @Test
    void describesItselfWithItsDelegateAndInterceptors() {
        java.util.Map<String, Object> properties = new java.util.HashMap<>();
        org.axonframework.common.infra.ComponentDescriptor descriptor =
                (org.axonframework.common.infra.ComponentDescriptor) java.lang.reflect.Proxy.newProxyInstance(
                        getClass().getClassLoader(),
                        new Class<?>[]{org.axonframework.common.infra.ComponentDescriptor.class},
                        (proxy, method, args) -> {
                            if (method.getName().equals("describeProperty") && args.length == 2) {
                                properties.put((String) args[0], args[1]);
                            } else if (method.getName().equals("describeWrapperOf")) {
                                properties.put("delegate", args[0]);
                            }
                            return null;
                        }
                );

        plainBus().describeTo(descriptor);

        assertSame(delegate, properties.get("delegate"));
        assertTrue(properties.containsKey("handlerInterceptors"));
        assertTrue(properties.containsKey("dispatchInterceptors"));
        assertTrue(properties.containsKey("updateDispatchInterceptors"));
    }

    @SuppressWarnings({"unused", "java:S119"})
    private QueryBus asQueryBus() {
        return plainBus();
    }
}
