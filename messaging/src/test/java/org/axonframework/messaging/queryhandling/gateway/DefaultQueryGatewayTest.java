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
package org.axonframework.messaging.queryhandling.gateway;

import org.axonframework.common.infra.ComponentDescriptor;
import org.axonframework.conversion.Converter;
import org.axonframework.messaging.core.EmptyApplicationContext;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.MessageTypeResolver;
import org.axonframework.messaging.core.Metadata;
import org.axonframework.messaging.core.conversion.DelegatingMessageConverter;
import org.axonframework.messaging.core.conversion.MessageConverter;
import org.axonframework.messaging.core.unitofwork.ProcessingContext;
import org.axonframework.messaging.core.unitofwork.SimpleUnitOfWorkFactory;
import org.axonframework.messaging.queryhandling.GenericQueryMessage;
import org.axonframework.messaging.queryhandling.GenericQueryResponseMessage;
import org.axonframework.messaging.queryhandling.GenericSubscriptionQueryUpdateMessage;
import org.axonframework.messaging.queryhandling.QueryMessage;
import org.axonframework.messaging.queryhandling.QueryPriorityCalculator;
import org.axonframework.messaging.queryhandling.SimpleQueryBus;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link DefaultQueryGateway}, using a real {@link SimpleQueryBus}.
 */
class DefaultQueryGatewayTest {

    private static final MessageType QUERY_TYPE = new MessageType("test.query");
    private static final MessageType RESPONSE_TYPE = new MessageType("test.response");
    private static final MessageType UPDATE_TYPE = new MessageType("test.update");

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

    private final SimpleQueryBus queryBus =
            new SimpleQueryBus(new SimpleUnitOfWorkFactory(EmptyApplicationContext.INSTANCE));
    private final MessageTypeResolver typeResolver = payloadType ->
            String.class.equals(payloadType) ? Optional.of(QUERY_TYPE) : Optional.empty();
    private final MessageConverter converter = new DelegatingMessageConverter(IDENTITY_CONVERTER);
    private final AtomicReference<QueryMessage> received = new AtomicReference<>();

    private DefaultQueryGateway gateway(QueryPriorityCalculator calculator) {
        return new DefaultQueryGateway(queryBus, typeResolver, calculator, converter);
    }

    private DefaultQueryGateway gateway() {
        return gateway(QueryPriorityCalculator.defaultCalculator());
    }

    private void handleWith(Object... responses) {
        queryBus.subscribe(QUERY_TYPE.qualifiedName(), (query, context) -> {
            received.set(query);
            MessageStream<org.axonframework.messaging.queryhandling.QueryResponseMessage> stream =
                    MessageStream.empty().cast();
            for (Object response : responses) {
                stream = stream.concatWith(MessageStream.just(new GenericQueryResponseMessage(RESPONSE_TYPE, response)));
            }
            return stream;
        });
    }

    @Test
    void constructorRejectsNullArguments() {
        QueryPriorityCalculator calculator = QueryPriorityCalculator.defaultCalculator();

        assertThrows(NullPointerException.class, () -> new DefaultQueryGateway(null, typeResolver, calculator, converter));
        assertThrows(NullPointerException.class, () -> new DefaultQueryGateway(queryBus, null, calculator, converter));
        assertThrows(NullPointerException.class, () -> new DefaultQueryGateway(queryBus, typeResolver, null, converter));
        assertThrows(NullPointerException.class,
                     () -> new DefaultQueryGateway(queryBus, typeResolver, calculator, null));
    }

    // --- query ---

    @Test
    void queryReturnsTheFirstResponsePayload() {
        handleWith("first", "second");

        String result = gateway().query("question", String.class, null).join();

        assertEquals("first", result);
        assertEquals("question", received.get().payload());
        assertEquals(QUERY_TYPE, received.get().type());
    }

    @Test
    void queryWithoutResponseCompletesWithNull() {
        handleWith();

        assertNull(gateway().query("question", String.class, null).join());
    }

    @Test
    void queryPassesTheGivenMetadataToTheHandler() {
        handleWith("answer");

        gateway().query("question", String.class, Metadata.with("key", "value"), null).join();

        assertEquals("value", received.get().metadata().get("key"));
    }

    @Test
    void queryWithoutHandlerFails() {
        CompletableFuture<String> result = gateway().query("question", String.class, null);

        assertTrue(result.isCompletedExceptionally());
    }

    @Test
    void queryForAnUnresolvableTypeFails() {
        assertThrows(RuntimeException.class, () -> gateway().query(42, String.class, null));
    }

    @Test
    void queryMessagesAreDispatchedAsIsAndMessagesKeepTheirType() {
        handleWith("answer");
        GenericQueryMessage message = new GenericQueryMessage(QUERY_TYPE, "wrapped");

        gateway().query(message, String.class, null).join();

        assertEquals("wrapped", received.get().payload());
        assertEquals(message.identifier(), received.get().identifier());
    }

    // --- queryMany ---

    @Test
    void queryManyCollectsAllResponses() {
        handleWith("a", "b", "c");

        List<String> result = gateway().queryMany("question", String.class, null).join();

        assertEquals(List.of("a", "b", "c"), result);
    }

    @Test
    void queryManyPassesMetadata() {
        handleWith("a");

        gateway().queryMany("question", String.class, Metadata.with("key", "value"), null).join();

        assertEquals("value", received.get().metadata().get("key"));
    }

    // --- streamingQuery ---

    @Test
    void streamingQueryEmitsAllResponses() {
        handleWith("a", "b");

        List<String> result = Flux.from(gateway().streamingQuery("question", String.class, null))
                                  .collectList().block();

        assertEquals(List.of("a", "b"), result);
    }

    @Test
    void streamingQueryPassesMetadata() {
        handleWith("a");

        Flux.from(gateway().streamingQuery("question", String.class, Metadata.with("key", "value"), null))
            .collectList().block();

        assertEquals("value", received.get().metadata().get("key"));
    }

    // --- subscriptionQuery ---

    @Test
    void subscriptionQueryEmitsTheInitialResponseAndUpdates() {
        handleWith("initial");
        Flux<String> flux = Flux.from(gateway().subscriptionQuery("question", String.class, (ProcessingContext) null, 8));

        queryBus.emitUpdate(q -> true,
                            () -> new GenericSubscriptionQueryUpdateMessage(UPDATE_TYPE, "update"),
                            null).join();
        queryBus.completeSubscriptions(q -> true, null).join();

        assertEquals(List.of("initial", "update"), flux.collectList().block());
    }

    @Test
    void subscriptionQueryWithMetadataPassesIt() {
        handleWith("initial");
        Flux<String> flux = Flux.from(
                gateway().subscriptionQuery("question", String.class, Metadata.with("key", "value"), null, 8)
        );
        queryBus.completeSubscriptions(q -> true, null).join();
        flux.collectList().block();

        assertEquals("value", received.get().metadata().get("key"));
    }

    @Test
    void subscriptionQueryWithCustomMapperAppliesIt() {
        handleWith("initial");
        Flux<Integer> flux = Flux.from(gateway().subscriptionQuery(
                "question", Integer.class, m -> m.payloadAs(String.class, converter).length(), null, 8
        ));
        queryBus.completeSubscriptions(q -> true, null).join();

        assertEquals(List.of("initial".length()), flux.collectList().block());
    }

    @Test
    void subscriptionQueryOverloadsWithoutContextWork() {
        handleWith("initial");
        Flux<String> defaultBuffer = Flux.from(gateway().subscriptionQuery("question", String.class));
        queryBus.completeSubscriptions(q -> true, null).join();
        assertEquals(List.of("initial"), defaultBuffer.collectList().block());

        Flux<String> sized = Flux.from(gateway().subscriptionQuery("question", String.class, 4));
        queryBus.completeSubscriptions(q -> true, null).join();
        assertEquals(List.of("initial"), sized.collectList().block());
    }

    // --- priority ---

    @Test
    void calculatedPriorityIsAppliedToDispatchedQueries() {
        handleWith("answer");

        gateway(query -> 7).query("question", String.class, null).join();

        assertEquals(OptionalInt.of(7), received.get().priority());
    }

    @Test
    void priorityIsAppliedToAllQueryVariants() {
        handleWith("answer");
        DefaultQueryGateway gateway = gateway(query -> 3);

        gateway.queryMany("question", String.class, null).join();
        assertEquals(OptionalInt.of(3), received.get().priority());

        received.set(null);
        Flux.from(gateway.streamingQuery("question", String.class, null)).collectList().block();
        assertEquals(OptionalInt.of(3), received.get().priority());

        received.set(null);
        Flux<String> subscription = Flux.from(gateway.subscriptionQuery("question", String.class, (ProcessingContext) null, 8));
        queryBus.completeSubscriptions(q -> true, null).join();
        subscription.collectList().block();
        assertEquals(OptionalInt.of(3), received.get().priority());
    }

    @Test
    void priorityAlreadySetOnTheQueryTakesPrecedence() {
        handleWith("answer");
        GenericQueryMessage message = new GenericQueryMessage(
                new GenericQueryMessage(QUERY_TYPE, "question"), 9
        );

        gateway(query -> 1).query(message, String.class, null).join();

        assertEquals(OptionalInt.of(9), received.get().priority());
    }

    @Test
    void priorityCalculatorIsOnlyConsultedForQueriesWithoutPriority() {
        handleWith("answer");
        AtomicReference<Integer> calls = new AtomicReference<>(0);
        QueryPriorityCalculator counting = query -> {
            calls.updateAndGet(c -> c + 1);
            return 1;
        };
        GenericQueryMessage withPriority = new GenericQueryMessage(new GenericQueryMessage(QUERY_TYPE, "q"), 9);

        gateway(counting).query(withPriority, String.class, null).join();

        assertEquals(0, calls.get());
    }

    // --- description ---

    @Test
    void describesItself() {
        Map<String, Object> properties = new HashMap<>();
        ComponentDescriptor descriptor = (ComponentDescriptor) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{ComponentDescriptor.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("describeProperty") && args.length == 2) {
                        properties.put((String) args[0], args[1]);
                    }
                    return null;
                }
        );
        QueryPriorityCalculator calculator = QueryPriorityCalculator.defaultCalculator();

        gateway(calculator).describeTo(descriptor);

        assertSame(queryBus, properties.get("queryBus"));
        assertSame(typeResolver, properties.get("messageTypeResolver"));
        assertSame(calculator, properties.get("priorityCalculator"));
        assertSame(converter, properties.get("converter"));
    }
}
