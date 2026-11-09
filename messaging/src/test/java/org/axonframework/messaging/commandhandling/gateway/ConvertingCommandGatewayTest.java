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
package org.axonframework.messaging.commandhandling.gateway;

import org.axonframework.common.infra.ComponentDescriptor;
import org.axonframework.conversion.Converter;
import org.axonframework.messaging.commandhandling.GenericCommandResultMessage;
import org.axonframework.messaging.core.EmptyApplicationContext;
import org.axonframework.messaging.core.Message;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.Metadata;
import org.axonframework.messaging.core.conversion.DelegatingMessageConverter;
import org.axonframework.messaging.core.conversion.MessageConverter;
import org.axonframework.messaging.core.unitofwork.ProcessingContext;
import org.axonframework.messaging.core.unitofwork.SimpleUnitOfWorkFactory;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test class validating the {@link ConvertingCommandGateway} and the {@link ContextAwareCommandDispatcher}.
 */
class ConvertingCommandGatewayTest {

    private static final MessageType RESULT_TYPE = new MessageType("test.result");

    /**
     * Converts {@link String} input to {@link Integer}, and otherwise passes the input through.
     */
    private static final Converter STRING_TO_INT_CONVERTER = new Converter() {
        @SuppressWarnings("unchecked")
        @Override
        public <T> @Nullable T convert(@Nullable Object input, Type targetType) {
            if (input instanceof String text && Integer.class.equals(targetType)) {
                return (T) Integer.valueOf(text);
            }
            return (T) input;
        }

        @Override
        public void describeTo(ComponentDescriptor descriptor) {
        }
    };

    private final MessageConverter converter = new DelegatingMessageConverter(STRING_TO_INT_CONVERTER);

    private final List<Object> sentCommands = new ArrayList<>();
    private final List<Metadata> sentMetadata = new ArrayList<>();
    private final List<ProcessingContext> sentContexts = new ArrayList<>();
    private CompletableFuture<? extends Message> response =
            CompletableFuture.completedFuture(new GenericCommandResultMessage(RESULT_TYPE, "21"));

    private final CommandGateway delegate = new CommandGateway() {
        @Override
        public CommandResult send(Object command, Metadata metadata, @Nullable ProcessingContext context) {
            sentCommands.add(command);
            sentMetadata.add(metadata);
            sentContexts.add(context);
            return new FutureCommandResult(response);
        }

        @Override
        public void describeTo(ComponentDescriptor descriptor) {
            descriptor.describeProperty("recording", true);
        }
    };

    private final ConvertingCommandGateway gateway = new ConvertingCommandGateway(delegate, converter);

    private static ComponentDescriptor recordingDescriptor(Map<String, Object> properties, List<Object> wrapped) {
        return (ComponentDescriptor) Proxy.newProxyInstance(
                ConvertingCommandGatewayTest.class.getClassLoader(),
                new Class<?>[]{ComponentDescriptor.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("describeProperty") && args.length == 2) {
                        properties.put((String) args[0], args[1]);
                    } else if (method.getName().equals("describeWrapperOf")) {
                        wrapped.add(args[0]);
                    }
                    return null;
                }
        );
    }

    @Test
    void constructorRejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> new ConvertingCommandGateway(null, converter));
        assertThrows(NullPointerException.class, () -> new ConvertingCommandGateway(delegate, null));
    }

    @Test
    void sendDelegatesCommandMetadataAndContext() {
        Metadata metadata = Metadata.with("k", "v");

        gateway.send("command", metadata, null);

        assertEquals(List.of("command"), sentCommands);
        assertEquals(List.of(metadata), sentMetadata);
        assertNull(sentContexts.get(0));
    }

    @Test
    void resultAsConvertsThePayloadUsingTheMessageConverter() {
        Integer result = gateway.send("command", Metadata.emptyInstance(), null).resultAs(Integer.class).join();

        assertEquals(21, result);
    }

    @Test
    void convenienceSendAndWaitUsesTheConvertingResult() {
        assertEquals(21, gateway.sendAndWait("command", Integer.class));
    }

    @Test
    void resultAsReturnsNullWhenThereIsNoResultMessage() {
        response = CompletableFuture.completedFuture(null);

        assertNull(gateway.send("command", Metadata.emptyInstance(), null).resultAs(Integer.class).join());
    }

    @Test
    void resultAsPropagatesFailuresOfTheDelegate() {
        response = CompletableFuture.failedFuture(new IllegalStateException("boom"));

        assertThrows(java.util.concurrent.CompletionException.class,
                     () -> gateway.send("command", Metadata.emptyInstance(), null).resultAs(Integer.class).join());
    }

    @Test
    void getResultMessageReturnsTheUnconvertedMessage() {
        Message message = gateway.send("command", Metadata.emptyInstance(), null).getResultMessage().join();

        assertEquals("21", message.payload());
    }

    @Test
    void onSuccessConvertsThePayloadBeforeInvokingTheHandler() {
        AtomicReference<Object> payload = new AtomicReference<>();
        AtomicReference<Message> message = new AtomicReference<>();

        gateway.send("command", Metadata.emptyInstance(), null).onSuccess(Integer.class, (p, m) -> {
            payload.set(p);
            message.set(m);
        });

        assertEquals(21, payload.get());
        assertEquals("21", message.get().payload());
    }

    @Test
    void onSuccessWithoutMessageInvokesTheHandlerWithNulls() {
        response = CompletableFuture.completedFuture(null);
        AtomicInteger invocations = new AtomicInteger();
        AtomicReference<Object> payload = new AtomicReference<>("sentinel");
        AtomicReference<Message> message = new AtomicReference<>(new GenericCommandResultMessage(RESULT_TYPE, "x"));

        gateway.send("command", Metadata.emptyInstance(), null).onSuccess(Integer.class, (p, m) -> {
            invocations.incrementAndGet();
            payload.set(p);
            message.set(m);
        });

        assertEquals(1, invocations.get());
        assertNull(payload.get());
        assertNull(message.get());
    }

    @Test
    void onSuccessIsNotInvokedWhenTheDelegateFails() {
        response = CompletableFuture.failedFuture(new IllegalStateException("boom"));
        AtomicInteger invocations = new AtomicInteger();
        AtomicInteger errors = new AtomicInteger();

        gateway.send("command", Metadata.emptyInstance(), null)
               .onSuccess(Integer.class, (p, m) -> invocations.incrementAndGet())
               .onError(e -> errors.incrementAndGet());

        assertEquals(0, invocations.get());
        assertEquals(1, errors.get());
    }

    @Test
    void onSuccessRejectsANullHandler() {
        CommandResult result = gateway.send("command", Metadata.emptyInstance(), null);

        assertThrows(NullPointerException.class,
                     () -> result.onSuccess(Integer.class, (java.util.function.BiConsumer<Integer, Message>) null));
    }

    @Test
    void describesItselfAsAWrapperOfTheDelegate() {
        Map<String, Object> properties = new HashMap<>();
        List<Object> wrapped = new ArrayList<>();

        gateway.describeTo(recordingDescriptor(properties, wrapped));

        assertEquals(List.of(delegate), wrapped);
        assertSame(converter, properties.get("converter"));
    }

    // --- ContextAwareCommandDispatcher ---

    private <T> T inContext(java.util.function.Function<ProcessingContext, T> action) {
        return new SimpleUnitOfWorkFactory(EmptyApplicationContext.INSTANCE)
                .create()
                .executeWithResult(context -> CompletableFuture.completedFuture(action.apply(context)))
                .join();
    }

    @Test
    void dispatcherRejectsNullArguments() {
        inContext(context -> {
            assertThrows(NullPointerException.class, () -> new ContextAwareCommandDispatcher(null, context));
            assertThrows(NullPointerException.class, () -> new ContextAwareCommandDispatcher(delegate, null));
            return null;
        });
    }

    @Test
    void dispatcherSendsInItsProcessingContext() {
        inContext(context -> {
            ContextAwareCommandDispatcher dispatcher = new ContextAwareCommandDispatcher(delegate, context);

            dispatcher.send("one");
            dispatcher.send("two", Metadata.with("k", "v"));

            assertEquals(List.of("one", "two"), sentCommands);
            assertEquals(Metadata.emptyInstance(), sentMetadata.get(0));
            assertEquals(Metadata.with("k", "v"), sentMetadata.get(1));
            assertSame(context, sentContexts.get(0));
            assertSame(context, sentContexts.get(1));
            return null;
        });
    }

    @Test
    void dispatcherSendWithResultTypeResolvesTheResult() {
        Integer result = inContext(context -> new ContextAwareCommandDispatcher(
                new ConvertingCommandGateway(delegate, converter), context)
                .send("command", Integer.class).join());

        assertEquals(21, result);
    }

    @Test
    void dispatcherDescribesItself() {
        inContext(context -> {
            Map<String, Object> properties = new HashMap<>();

            new ContextAwareCommandDispatcher(delegate, context)
                    .describeTo(recordingDescriptor(properties, new ArrayList<>()));

            assertSame(context, properties.get("processingContext"));
            assertSame(delegate, properties.get("commandGateway"));
            return null;
        });
    }
}
