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
package org.axonframework.messaging.commandhandling.interception;

import org.axonframework.common.infra.ComponentDescriptor;
import org.axonframework.messaging.commandhandling.CommandHandler;
import org.axonframework.messaging.commandhandling.CommandMessage;
import org.axonframework.messaging.commandhandling.CommandResultMessage;
import org.axonframework.messaging.commandhandling.GenericCommandMessage;
import org.axonframework.messaging.commandhandling.GenericCommandResultMessage;
import org.axonframework.messaging.commandhandling.NoHandlerForCommandException;
import org.axonframework.messaging.commandhandling.SimpleCommandBus;
import org.axonframework.messaging.commandhandling.SimpleCommandHandlingComponent;
import org.axonframework.messaging.core.EmptyApplicationContext;
import org.axonframework.messaging.core.MessageDispatchInterceptor;
import org.axonframework.messaging.core.MessageHandlerInterceptor;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.Metadata;
import org.axonframework.messaging.core.QualifiedName;
import org.axonframework.messaging.core.unitofwork.SimpleUnitOfWorkFactory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link InterceptingCommandBus}, the {@link InterceptingCommandHandlingComponent} and the
 * {@link CommandMessageHandlerInterceptorChain}.
 */
class InterceptingCommandBusTest {

    private static final MessageType TYPE = new MessageType("test.command");
    private static final QualifiedName NAME = TYPE.qualifiedName();

    private final SimpleUnitOfWorkFactory unitOfWorkFactory = new SimpleUnitOfWorkFactory(EmptyApplicationContext.INSTANCE);
    private final SimpleCommandBus delegate = new SimpleCommandBus(unitOfWorkFactory);
    private final List<String> events = new ArrayList<>();

    private static CommandMessage command() {
        return new GenericCommandMessage(TYPE, "payload");
    }

    private static CommandHandler replying(Object result) {
        return (command, context) -> MessageStream.just(
                new GenericCommandResultMessage(new MessageType("test.result"), result)
        );
    }

    @SafeVarargs
    private static List<MessageHandlerInterceptor<? super CommandMessage>> handlerInterceptors(
            MessageHandlerInterceptor<? super CommandMessage>... interceptors
    ) {
        return List.of(interceptors);
    }

    @SafeVarargs
    private static List<MessageDispatchInterceptor<? super CommandMessage>> dispatchInterceptors(
            MessageDispatchInterceptor<? super CommandMessage>... interceptors
    ) {
        return List.of(interceptors);
    }

    private MessageHandlerInterceptor<CommandMessage> recordingHandlerInterceptor(String name) {
        return (message, context, chain) -> {
            events.add(name + ":before");
            return chain.proceed(message, context);
        };
    }

    private MessageDispatchInterceptor<CommandMessage> recordingDispatchInterceptor(String name) {
        return (message, context, chain) -> {
            events.add(name + ":dispatch");
            return chain.proceed(message, context);
        };
    }

    private static ComponentDescriptor recordingDescriptor(Map<String, Object> properties, List<Object> wrapped) {
        return (ComponentDescriptor) Proxy.newProxyInstance(
                InterceptingCommandBusTest.class.getClassLoader(),
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

    // --- InterceptingCommandBus ---

    @Test
    void busConstructorRejectsNullArguments() {
        assertThrows(NullPointerException.class,
                     () -> new InterceptingCommandBus(null, handlerInterceptors(), dispatchInterceptors()));
        assertThrows(NullPointerException.class,
                     () -> new InterceptingCommandBus(delegate, null, dispatchInterceptors()));
        assertThrows(NullPointerException.class,
                     () -> new InterceptingCommandBus(delegate, handlerInterceptors(), null));
    }

    @Test
    void busConstructorMessageNamesTheMissingDelegate() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new InterceptingCommandBus(null, handlerInterceptors(), dispatchInterceptors())
        );

        assertTrue(exception.getMessage().contains("must not be null"));
    }

    @Test
    void busWithoutInterceptorsDispatchesStraightToTheDelegate() {
        InterceptingCommandBus bus = new InterceptingCommandBus(delegate, handlerInterceptors(), dispatchInterceptors());
        bus.subscribe(NAME, replying("done"));

        assertEquals("done", bus.dispatch(command(), null).join().payload());
    }

    @Test
    void busSubscribeReturnsTheInterceptingBus() {
        InterceptingCommandBus bus = new InterceptingCommandBus(delegate, handlerInterceptors(), dispatchInterceptors());

        assertSame(bus, bus.subscribe(NAME, replying("done")));
    }

    @Test
    void busInvokesDispatchInterceptorsInOrderBeforeTheDelegate() {
        InterceptingCommandBus bus = new InterceptingCommandBus(
                delegate,
                handlerInterceptors(),
                dispatchInterceptors(recordingDispatchInterceptor("one"), recordingDispatchInterceptor("two"))
        );
        bus.subscribe(NAME, (command, context) -> {
            events.add("handler");
            return MessageStream.just(new GenericCommandResultMessage(new MessageType("test.result"), "done"));
        });

        bus.dispatch(command(), null).join();

        assertEquals(List.of("one:dispatch", "two:dispatch", "handler"), events);
    }

    @Test
    void busInvokesHandlerInterceptorsInOrderAroundTheHandler() {
        InterceptingCommandBus bus = new InterceptingCommandBus(
                delegate,
                handlerInterceptors(recordingHandlerInterceptor("one"), recordingHandlerInterceptor("two")),
                dispatchInterceptors()
        );
        bus.subscribe(NAME, (command, context) -> {
            events.add("handler");
            return MessageStream.just(new GenericCommandResultMessage(new MessageType("test.result"), "done"));
        });

        bus.dispatch(command(), null).join();

        assertEquals(List.of("one:before", "two:before", "handler"), events);
    }

    @Test
    void busDispatchInterceptorsCanEnrichTheCommand() {
        MessageDispatchInterceptor<CommandMessage> enriching = (message, context, chain) ->
                chain.proceed(message.andMetadata(Metadata.with("enriched", "yes")), context);
        InterceptingCommandBus bus = new InterceptingCommandBus(
                delegate, handlerInterceptors(), dispatchInterceptors(enriching)
        );
        bus.subscribe(NAME, (command, context) -> MessageStream.just(
                new GenericCommandResultMessage(new MessageType("test.result"), command.metadata().get("enriched"))
        ));

        assertEquals("yes", bus.dispatch(command(), null).join().payload());
    }

    @Test
    void busDispatchInterceptorsCanRejectTheCommand() {
        IllegalStateException rejection = new IllegalStateException("rejected");
        MessageDispatchInterceptor<CommandMessage> rejecting = (message, context, chain) ->
                MessageStream.failed(rejection);
        InterceptingCommandBus bus = new InterceptingCommandBus(
                delegate, handlerInterceptors(), dispatchInterceptors(rejecting)
        );
        bus.subscribe(NAME, (command, context) -> {
            events.add("handler");
            return MessageStream.just(new GenericCommandResultMessage(new MessageType("test.result"), "done"));
        });

        CompletionException failure = assertThrows(CompletionException.class, () -> bus.dispatch(command(), null).join());

        assertSame(rejection, failure.getCause());
        assertTrue(events.isEmpty());
    }

    @Test
    void busHandlerInterceptorsCanShortCircuitTheHandler() {
        IllegalStateException rejection = new IllegalStateException("rejected");
        MessageHandlerInterceptor<CommandMessage> rejecting = (message, context, chain) ->
                MessageStream.failed(rejection);
        InterceptingCommandBus bus = new InterceptingCommandBus(
                delegate, handlerInterceptors(rejecting), dispatchInterceptors()
        );
        bus.subscribe(NAME, (command, context) -> {
            events.add("handler");
            return MessageStream.just(new GenericCommandResultMessage(new MessageType("test.result"), "done"));
        });

        CompletionException failure = assertThrows(CompletionException.class, () -> bus.dispatch(command(), null).join());

        assertSame(rejection, failure.getCause());
        assertTrue(events.isEmpty());
    }

    @Test
    void busDispatchWithoutHandlerFailsWithNoHandlerForCommandException() {
        InterceptingCommandBus bus = new InterceptingCommandBus(delegate, handlerInterceptors(), dispatchInterceptors());

        CompletionException failure = assertThrows(CompletionException.class, () -> bus.dispatch(command(), null).join());

        assertInstanceOf(NoHandlerForCommandException.class, failure.getCause());
    }

    @Test
    void busCopiesTheInterceptorListsGivenAtConstruction() {
        List<MessageDispatchInterceptor<? super CommandMessage>> dispatchList = new ArrayList<>();
        List<MessageHandlerInterceptor<? super CommandMessage>> handlerList = new ArrayList<>();
        InterceptingCommandBus bus = new InterceptingCommandBus(delegate, handlerList, dispatchList);
        bus.subscribe(NAME, replying("done"));

        dispatchList.add(recordingDispatchInterceptor("late"));
        handlerList.add(recordingHandlerInterceptor("late"));
        bus.dispatch(command(), null).join();

        assertTrue(events.isEmpty());
    }

    @Test
    void busDescribesItself() {
        List<MessageHandlerInterceptor<? super CommandMessage>> handlers = handlerInterceptors(recordingHandlerInterceptor("a"));
        Map<String, Object> properties = new HashMap<>();
        List<Object> wrapped = new ArrayList<>();

        new InterceptingCommandBus(delegate, handlers, dispatchInterceptors())
                .describeTo(recordingDescriptor(properties, wrapped));

        assertEquals(List.of(delegate), wrapped);
        assertEquals(handlers, properties.get("handlerInterceptors"));
        assertEquals(List.of(), properties.get("dispatchInterceptors"));
    }

    // --- InterceptingCommandHandlingComponent ---

    private SimpleCommandHandlingComponent component(CommandHandler handler) {
        SimpleCommandHandlingComponent component = SimpleCommandHandlingComponent.create("component");
        component.subscribe(NAME, handler);
        return component;
    }

    private CommandResultMessage handle(InterceptingCommandHandlingComponent component) {
        return unitOfWorkFactory.create()
                                .executeWithResult(context -> component.handle(command(), context)
                                                                       .asCompletableFuture())
                                .join()
                                .message();
    }

    @Test
    void componentConstructorRejectsNullArguments() {
        SimpleCommandHandlingComponent component = component(replying("done"));

        assertThrows(NullPointerException.class, () -> new InterceptingCommandHandlingComponent(handlerInterceptors(), null));
        assertThrows(NullPointerException.class, () -> new InterceptingCommandHandlingComponent(null, component));
    }

    @Test
    void componentRunsInterceptorsInOrderAroundTheDelegate() {
        InterceptingCommandHandlingComponent component = new InterceptingCommandHandlingComponent(
                handlerInterceptors(recordingHandlerInterceptor("one"), recordingHandlerInterceptor("two")),
                component((command, context) -> {
                    events.add("handler");
                    return MessageStream.just(new GenericCommandResultMessage(new MessageType("test.result"), "done"));
                })
        );

        assertEquals("done", handle(component).payload());
        assertEquals(List.of("one:before", "two:before", "handler"), events);
    }

    @Test
    void componentWithoutInterceptorsDelegatesDirectly() {
        InterceptingCommandHandlingComponent component =
                new InterceptingCommandHandlingComponent(handlerInterceptors(), component(replying("done")));

        assertEquals("done", handle(component).payload());
    }

    @Test
    void componentReportsTheDelegatesSupportedCommands() {
        InterceptingCommandHandlingComponent component =
                new InterceptingCommandHandlingComponent(handlerInterceptors(), component(replying("done")));

        assertEquals(Set.of(NAME), component.supportedCommands());
    }

    @Test
    void componentDescribesItself() {
        SimpleCommandHandlingComponent wrapped = component(replying("done"));
        List<MessageHandlerInterceptor<? super CommandMessage>> interceptors = handlerInterceptors(recordingHandlerInterceptor("a"));
        Map<String, Object> properties = new HashMap<>();
        List<Object> wrappedComponents = new ArrayList<>();

        new InterceptingCommandHandlingComponent(interceptors, wrapped)
                .describeTo(recordingDescriptor(properties, wrappedComponents));

        assertEquals(List.of(wrapped), wrappedComponents);
        assertEquals(interceptors, properties.get("interceptors"));
    }

    // --- CommandMessageHandlerInterceptorChain ---

    @Test
    void chainRejectsANullHandler() {
        assertThrows(NullPointerException.class, () -> new CommandMessageHandlerInterceptorChain(handlerInterceptors(), null));
    }

    @Test
    void chainConvertsAnInterceptorExceptionIntoAFailedStream() {
        IllegalStateException error = new IllegalStateException("boom");
        MessageHandlerInterceptor<CommandMessage> throwing = (message, context, chain) -> {
            throw error;
        };
        CommandMessageHandlerInterceptorChain chain =
                new CommandMessageHandlerInterceptorChain(handlerInterceptors(throwing), replying("done"));

        MessageStream<?> result = chain.proceed(command(), null);

        assertSame(error, result.error().orElseThrow());
    }

    @Test
    void chainConvertsAHandlerExceptionIntoAFailedStream() {
        IllegalStateException error = new IllegalStateException("boom");
        CommandMessageHandlerInterceptorChain chain = new CommandMessageHandlerInterceptorChain(
                handlerInterceptors(), (command, context) -> {
            throw error;
        });

        assertSame(error, chain.proceed(command(), null).error().orElseThrow());
    }

    @Test
    void chainSnapshotsTheInterceptorsAtConstruction() {
        List<MessageHandlerInterceptor<? super CommandMessage>> interceptors = new ArrayList<>();
        CommandMessageHandlerInterceptorChain chain = new CommandMessageHandlerInterceptorChain(interceptors, replying("done"));

        interceptors.add(recordingHandlerInterceptor("late"));
        chain.proceed(command(), null);

        assertTrue(events.isEmpty());
    }
}
