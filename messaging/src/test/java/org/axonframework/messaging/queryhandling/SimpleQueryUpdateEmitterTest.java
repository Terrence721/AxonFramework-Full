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
import org.axonframework.conversion.Converter;
import org.axonframework.messaging.core.ApplicationContext;
import org.axonframework.messaging.core.GenericMessage;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.MessageTypeNotResolvedException;
import org.axonframework.messaging.core.MessageTypeResolver;
import org.axonframework.messaging.core.QualifiedName;
import org.axonframework.messaging.core.annotation.ParameterResolver;
import org.axonframework.messaging.core.conversion.DelegatingMessageConverter;
import org.axonframework.messaging.core.conversion.MessageConverter;
import org.axonframework.messaging.core.unitofwork.ProcessingContext;
import org.axonframework.messaging.core.unitofwork.SimpleUnitOfWorkFactory;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link SimpleQueryUpdateEmitter} and the
 * {@link QueryUpdateEmitterParameterResolverFactory}, using a real {@link SimpleQueryBus}.
 */
class SimpleQueryUpdateEmitterTest {

    private static final MessageType QUERY_TYPE = new MessageType("test.query");
    private static final MessageType UPDATE_TYPE = new MessageType("test.update");
    private static final QualifiedName QUERY_NAME = QUERY_TYPE.qualifiedName();

    private static final Converter IDENTITY_CONVERTER = new Converter() {
        @SuppressWarnings("unchecked")
        @Override
        public <T> @Nullable T convert(@Nullable Object input, Type targetType) {
            return (T) input;
        }

        @Override
        public void describeTo(ComponentDescriptor descriptor) {
        }
    };

    private final AtomicInteger resolverCalls = new AtomicInteger();
    private final MessageTypeResolver typeResolver = payloadType -> {
        resolverCalls.incrementAndGet();
        if (String.class.equals(payloadType)) {
            return Optional.of(QUERY_TYPE);
        }
        return Integer.class.equals(payloadType) ? Optional.of(UPDATE_TYPE) : Optional.empty();
    };
    private final MessageConverter converter = new DelegatingMessageConverter(IDENTITY_CONVERTER);
    private final Map<Class<?>, Object> components = new HashMap<>();
    private final SimpleQueryBus queryBus;
    private final SimpleUnitOfWorkFactory unitOfWorkFactory;

    SimpleQueryUpdateEmitterTest() {
        ApplicationContext applicationContext = new ApplicationContext() {
            @SuppressWarnings("unchecked")
            @Override
            public <C> C component(Class<C> type, @Nullable String name) {
                return (C) components.get(type);
            }
        };
        unitOfWorkFactory = new SimpleUnitOfWorkFactory(applicationContext);
        queryBus = new SimpleQueryBus(unitOfWorkFactory);
        components.put(QueryBus.class, queryBus);
        components.put(MessageTypeResolver.class, typeResolver);
        components.put(MessageConverter.class, converter);
    }

    private void inUnitOfWork(Consumer<ProcessingContext> action) {
        unitOfWorkFactory.create()
                         .executeWithResult(context -> {
                             action.accept(context);
                             return CompletableFuture.completedFuture(null);
                         })
                         .join();
    }

    private MessageStream<SubscriptionQueryUpdateMessage> subscribe(String payload) {
        return queryBus.subscribeToUpdates(new GenericQueryMessage(QUERY_TYPE, payload), 10);
    }

    @Test
    void emitByTypeDeliversUpdateToMatchingQueriesOnly() {
        MessageStream<SubscriptionQueryUpdateMessage> matching = subscribe("matching");
        MessageStream<SubscriptionQueryUpdateMessage> other = subscribe("other");

        inUnitOfWork(context -> new SimpleQueryUpdateEmitter(queryBus, typeResolver, converter, context)
                .emit(String.class, "matching"::equals, 42));

        SubscriptionQueryUpdateMessage update = matching.next().orElseThrow().message();
        assertEquals(42, update.payload());
        assertEquals(UPDATE_TYPE, update.type());
        assertFalse(other.hasNextAvailable());
    }

    @Test
    void emitByNameDeliversUpdateToMatchingQueriesOnly() {
        MessageStream<SubscriptionQueryUpdateMessage> matching = subscribe("matching");
        MessageStream<SubscriptionQueryUpdateMessage> other = subscribe("other");

        inUnitOfWork(context -> new SimpleQueryUpdateEmitter(queryBus, typeResolver, converter, context)
                .emit(QUERY_NAME, "other"::equals, 7));

        assertEquals(7, other.next().orElseThrow().message().payload());
        assertFalse(matching.hasNextAvailable());
    }

    @Test
    void updateSupplierIsNotInvokedWithoutMatchingQueries() {
        subscribe("other");
        AtomicInteger supplierCalls = new AtomicInteger();

        inUnitOfWork(context -> new SimpleQueryUpdateEmitter(queryBus, typeResolver, converter, context)
                .emit(String.class, "matching"::equals, () -> {
                    supplierCalls.incrementAndGet();
                    return 1;
                }));

        assertEquals(0, supplierCalls.get());
    }

    @Test
    void updateMessagesAreEmittedAsIs() {
        MessageStream<SubscriptionQueryUpdateMessage> updates = subscribe("matching");
        SubscriptionQueryUpdateMessage update = new GenericSubscriptionQueryUpdateMessage(UPDATE_TYPE, "update");

        inUnitOfWork(context -> new SimpleQueryUpdateEmitter(queryBus, typeResolver, converter, context)
                .emit(QUERY_NAME, ignored -> true, update));

        assertSame(update, updates.next().orElseThrow().message());
    }

    @Test
    void plainMessagesAreWrappedAsUpdatesKeepingTheirIdentifierAndType() {
        MessageStream<SubscriptionQueryUpdateMessage> updates = subscribe("matching");
        GenericMessage message = new GenericMessage(UPDATE_TYPE, "update");

        inUnitOfWork(context -> new SimpleQueryUpdateEmitter(queryBus, typeResolver, converter, context)
                .emit(QUERY_NAME, ignored -> true, message));

        SubscriptionQueryUpdateMessage update = updates.next().orElseThrow().message();
        assertEquals(message.identifier(), update.identifier());
        assertEquals(UPDATE_TYPE, update.type());
        assertEquals("update", update.payload());
    }

    @Test
    void emitAndCountReturnsTheNumberOfMatchingQueries() {
        subscribe("matching");
        AtomicReference<OptionalInt> byType = new AtomicReference<>();
        AtomicReference<OptionalInt> byName = new AtomicReference<>();

        inUnitOfWork(context -> {
            SimpleQueryUpdateEmitter emitter = new SimpleQueryUpdateEmitter(queryBus, typeResolver, converter, context);
            byType.set(emitter.emitAndCount(String.class, "matching"::equals, () -> 1));
            byName.set(emitter.emitAndCount(QUERY_NAME, "none"::equals, () -> 1));
        });

        assertEquals(OptionalInt.of(1), byType.get());
        assertEquals(OptionalInt.of(0), byName.get());
    }

    @Test
    void completeOnlyCompletesMatchingQueries() {
        MessageStream<SubscriptionQueryUpdateMessage> matching = subscribe("matching");
        MessageStream<SubscriptionQueryUpdateMessage> other = subscribe("other");
        AtomicReference<OptionalInt> count = new AtomicReference<>();

        inUnitOfWork(context -> {
            SimpleQueryUpdateEmitter emitter = new SimpleQueryUpdateEmitter(queryBus, typeResolver, converter, context);
            emitter.complete(String.class, "matching"::equals);
            count.set(emitter.completeAndCount(QUERY_NAME, "other"::equals));
        });

        assertEquals(OptionalInt.of(1), count.get());
        assertTrue(matching.next().isEmpty());
        assertTrue(matching.isCompleted());
        assertTrue(other.next().isEmpty());
        assertTrue(other.isCompleted());
    }

    @Test
    void completeExceptionallyPropagatesTheCauseToMatchingQueries() {
        MessageStream<SubscriptionQueryUpdateMessage> byType = subscribe("by-type");
        MessageStream<SubscriptionQueryUpdateMessage> byName = subscribe("by-name");
        MessageStream<SubscriptionQueryUpdateMessage> untouched = subscribe("untouched");
        RuntimeException typeCause = new RuntimeException("type");
        RuntimeException nameCause = new RuntimeException("name");

        inUnitOfWork(context -> {
            SimpleQueryUpdateEmitter emitter = new SimpleQueryUpdateEmitter(queryBus, typeResolver, converter, context);
            emitter.completeExceptionally(String.class, "by-type"::equals, typeCause);
            emitter.completeExceptionally(QUERY_NAME, "by-name"::equals, nameCause);
        });

        assertTrue(byType.next().isEmpty());
        assertSame(typeCause, byType.error().orElseThrow());
        assertTrue(byName.next().isEmpty());
        assertSame(nameCause, byName.error().orElseThrow());
        assertFalse(untouched.isCompleted());
    }

    @Test
    void completeExceptionallyAndCountReturnsTheNumberOfMatchingQueries() {
        subscribe("matching");
        AtomicReference<OptionalInt> byName = new AtomicReference<>();

        inUnitOfWork(context -> byName.set(
                new SimpleQueryUpdateEmitter(queryBus, typeResolver, converter, context)
                        .completeExceptionallyAndCount(QUERY_NAME, ignored -> true, new RuntimeException("failure"))
        ));

        assertEquals(OptionalInt.of(1), byName.get());
    }

    @Test
    void unresolvableQueryTypeFailsEagerlyEvenWithoutSubscriptions() {
        inUnitOfWork(context -> {
            SimpleQueryUpdateEmitter emitter = new SimpleQueryUpdateEmitter(queryBus, typeResolver, converter, context);

            assertThrows(MessageTypeNotResolvedException.class,
                         () -> emitter.emit(Double.class, ignored -> true, 1));
            assertThrows(MessageTypeNotResolvedException.class,
                         () -> emitter.complete(Double.class, ignored -> true));
        });
    }

    @Test
    void queryTypeIsResolvedOnceRegardlessOfTheNumberOfSubscriptions() {
        subscribe("one");
        subscribe("two");
        subscribe("three");

        inUnitOfWork(context -> {
            resolverCalls.set(0);
            new SimpleQueryUpdateEmitter(queryBus, typeResolver, converter, context)
                    .complete(String.class, ignored -> false);
            assertEquals(1, resolverCalls.get());
        });
    }

    @Test
    void constructorRejectsNullArguments() {
        inUnitOfWork(context -> {
            assertThrows(NullPointerException.class,
                         () -> new SimpleQueryUpdateEmitter(null, typeResolver, converter, context));
            assertThrows(NullPointerException.class,
                         () -> new SimpleQueryUpdateEmitter(queryBus, null, converter, context));
            assertThrows(NullPointerException.class,
                         () -> new SimpleQueryUpdateEmitter(queryBus, typeResolver, null, context));
            assertThrows(NullPointerException.class,
                         () -> new SimpleQueryUpdateEmitter(queryBus, typeResolver, converter, null));
        });
    }

    @Test
    void describesItsCollaborators() {
        Map<String, Object> described = new HashMap<>();
        ComponentDescriptor descriptor = (ComponentDescriptor) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{ComponentDescriptor.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("describeProperty")) {
                        described.put((String) args[0], args[1]);
                    }
                    return null;
                });

        inUnitOfWork(context -> new SimpleQueryUpdateEmitter(queryBus, typeResolver, converter, context)
                .describeTo(descriptor));

        assertSame(queryBus, described.get("queryBus"));
        assertSame(typeResolver, described.get("messageTypeResolver"));
        assertSame(converter, described.get("converter"));
        assertNotNull(described.get("context"));
    }

    @Test
    void forContextCreatesAFreshEmitterFromTheContextComponents() {
        subscribe("matching");
        AtomicReference<QueryUpdateEmitter> first = new AtomicReference<>();
        AtomicReference<QueryUpdateEmitter> second = new AtomicReference<>();
        AtomicReference<OptionalInt> count = new AtomicReference<>();

        inUnitOfWork(context -> {
            first.set(QueryUpdateEmitter.forContext(context));
            second.set(QueryUpdateEmitter.forContext(context));
            count.set(first.get().emitAndCount(QUERY_NAME, ignored -> true, () -> 1));
        });

        assertInstanceOf(SimpleQueryUpdateEmitter.class, first.get());
        assertFalse(first.get() == second.get());
        assertEquals(OptionalInt.of(1), count.get());
    }

    // --- QueryUpdateEmitterParameterResolverFactory ---

    @SuppressWarnings("java:S119")
    void handler(QueryUpdateEmitter emitter, String other) {
    }

    private Method handlerMethod() throws NoSuchMethodException {
        return SimpleQueryUpdateEmitterTest.class.getDeclaredMethod("handler", QueryUpdateEmitter.class, String.class);
    }

    @Test
    void parameterResolverFactoryOnlyResolvesQueryUpdateEmitterParameters() throws NoSuchMethodException {
        QueryUpdateEmitterParameterResolverFactory factory = new QueryUpdateEmitterParameterResolverFactory();
        Method handler = handlerMethod();

        assertNotNull(factory.createInstance(handler, handler.getParameters(), 0));
        assertNull(factory.createInstance(handler, handler.getParameters(), 1));
    }

    @Test
    void parameterResolverCreatesAnEmitterBoundToTheProcessingContext() throws NoSuchMethodException {
        Method handler = handlerMethod();
        ParameterResolver<QueryUpdateEmitter> resolver =
                new QueryUpdateEmitterParameterResolverFactory().createInstance(handler, handler.getParameters(), 0);
        AtomicReference<QueryUpdateEmitter> resolved = new AtomicReference<>();
        AtomicReference<Boolean> matches = new AtomicReference<>();

        inUnitOfWork(context -> {
            matches.set(resolver.matches(context));
            resolved.set(resolver.resolveParameterValue(context).join());
        });

        assertTrue(matches.get());
        assertInstanceOf(SimpleQueryUpdateEmitter.class, resolved.get());
    }
}
