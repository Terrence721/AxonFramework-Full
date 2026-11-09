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
package org.axonframework.messaging.commandhandling;

import org.axonframework.messaging.core.EmptyApplicationContext;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.QualifiedName;
import org.axonframework.messaging.core.unitofwork.SimpleUnitOfWorkFactory;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link SimpleCommandBus} and the {@link SimpleCommandHandlingComponent}.
 */
class SimpleCommandBusTest {

    private static final MessageType TYPE = new MessageType("test.command");
    private static final QualifiedName NAME = TYPE.qualifiedName();

    private final SimpleCommandBus bus = new SimpleCommandBus(new SimpleUnitOfWorkFactory(EmptyApplicationContext.INSTANCE));

    private static CommandMessage command() {
        return new GenericCommandMessage(TYPE, "payload");
    }

    private static CommandHandler replying(Object result) {
        return (command, context) -> MessageStream.just(
                new GenericCommandResultMessage(new MessageType("test.result"), result)
        );
    }

    // --- SimpleCommandBus ---

    @Test
    void constructorRejectsANullUnitOfWorkFactory() {
        assertThrows(NullPointerException.class, () -> new SimpleCommandBus(null));
    }

    @Test
    void subscribeRejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> bus.subscribe(NAME, null));
        assertThrows(NullPointerException.class, () -> bus.subscribe((QualifiedName) null, replying("x")));
    }

    @Test
    void dispatchHandsTheCommandToTheSubscribedHandlerAndReturnsItsResult() {
        AtomicReference<CommandMessage> received = new AtomicReference<>();
        bus.subscribe(NAME, (command, context) -> {
            received.set(command);
            return MessageStream.just(new GenericCommandResultMessage(new MessageType("test.result"), "done"));
        });
        CommandMessage command = command();

        CommandResultMessage result = bus.dispatch(command, null).join();

        assertEquals("done", result.payload());
        assertSame(command, received.get());
    }

    @Test
    void dispatchRunsTheHandlerInAUnitOfWorkNamedAfterTheCommand() {
        AtomicReference<Object> context = new AtomicReference<>();
        bus.subscribe(NAME, (command, processingContext) -> {
            context.set(processingContext);
            return MessageStream.just(new GenericCommandResultMessage(new MessageType("test.result"), "done"));
        });

        bus.dispatch(command(), null).join();

        assertTrue(context.get() != null);
    }

    @Test
    void dispatchCompletesWithNullWhenTheHandlerReturnsNoResult() {
        bus.subscribe(NAME, (command, context) -> MessageStream.just(null));

        assertNull(bus.dispatch(command(), null).join());
    }

    @Test
    void dispatchWithoutHandlerFailsWithNoHandlerForCommandException() {
        CompletionException failure = assertThrows(CompletionException.class,
                                                   () -> bus.dispatch(command(), null).join());

        assertInstanceOf(NoHandlerForCommandException.class, failure.getCause());
        assertTrue(failure.getCause().getMessage().contains("test.command"));
    }

    @Test
    void dispatchFailsWhenTheHandlerFails() {
        IllegalStateException error = new IllegalStateException("boom");
        bus.subscribe(NAME, (command, context) -> MessageStream.failed(error));

        CompletionException failure = assertThrows(CompletionException.class,
                                                   () -> bus.dispatch(command(), null).join());

        assertSame(error, failure.getCause());
    }

    @Test
    void dispatchFailsWhenTheHandlerThrows() {
        bus.subscribe(NAME, (command, context) -> {
            throw new IllegalStateException("boom");
        });

        CompletionException failure = assertThrows(CompletionException.class,
                                                   () -> bus.dispatch(command(), null).join());

        assertInstanceOf(IllegalStateException.class, failure.getCause());
    }

    @Test
    void subscribingADifferentHandlerForTheSameNameIsRejected() {
        bus.subscribe(NAME, replying("one"));

        assertThrows(DuplicateCommandHandlerSubscriptionException.class, () -> bus.subscribe(NAME, replying("two")));
    }

    @Test
    void subscribingTheSameHandlerTwiceIsAllowed() {
        CommandHandler handler = replying("one");
        bus.subscribe(NAME, handler);

        assertSame(bus, bus.subscribe(NAME, handler));
    }

    @Test
    void subscribeReturnsTheBusForChaining() {
        assertSame(bus, bus.subscribe(NAME, replying("one")));
    }

    @Test
    void subscribeWithMultipleNamesSubscribesTheHandlerForEach() {
        CommandHandler handler = replying("shared");
        QualifiedName other = new QualifiedName("other.command");

        bus.subscribe(Set.of(NAME, other), handler);

        assertEquals("shared", bus.dispatch(command(), null).join().payload());
        assertEquals("shared",
                     bus.dispatch(new GenericCommandMessage(new MessageType("other.command"), "x"), null)
                        .join().payload());
    }

    // --- SimpleCommandHandlingComponent ---

    @Test
    void componentRejectsAnEmptyName() {
        assertThrows(IllegalArgumentException.class, () -> SimpleCommandHandlingComponent.create(""));
        assertThrows(IllegalArgumentException.class, () -> SimpleCommandHandlingComponent.create(null));
    }

    private CommandResultMessage handle(CommandHandlingComponent component, CommandMessage command) {
        return new SimpleUnitOfWorkFactory(EmptyApplicationContext.INSTANCE)
                .create()
                .executeWithResult(context -> component.handle(command, context).first().asCompletableFuture())
                .join()
                .message();
    }

    @Test
    void componentHandlesCommandsWithASubscribedHandler() {
        SimpleCommandHandlingComponent component = SimpleCommandHandlingComponent.create("component");
        component.subscribe(NAME, replying("handled"));

        assertEquals("handled", handle(component, command()).payload());
        assertEquals(Set.of(NAME), component.supportedCommands());
    }

    @Test
    void componentPrefersSubComponentsOverOwnHandlers() {
        SimpleCommandHandlingComponent sub = SimpleCommandHandlingComponent.create("sub");
        sub.subscribe(NAME, replying("sub"));
        SimpleCommandHandlingComponent component = SimpleCommandHandlingComponent.create("component");
        component.subscribe(NAME, replying("own"));
        component.subscribe(sub);

        assertEquals("sub", handle(component, command()).payload());
    }

    @Test
    void componentSubscribingAComponentAsHandlerRegistersItAsSubComponent() {
        SimpleCommandHandlingComponent sub = SimpleCommandHandlingComponent.create("sub");
        sub.subscribe(NAME, replying("sub"));
        SimpleCommandHandlingComponent component = SimpleCommandHandlingComponent.create("component");

        component.subscribe(new QualifiedName("ignored"), sub);

        assertEquals(Set.of(NAME), component.supportedCommands());
        assertEquals("sub", handle(component, command()).payload());
    }

    @Test
    void componentSupportedCommandsCombinesHandlersAndSubComponents() {
        QualifiedName other = new QualifiedName("other.command");
        SimpleCommandHandlingComponent sub = SimpleCommandHandlingComponent.create("sub");
        sub.subscribe(other, replying("sub"));
        SimpleCommandHandlingComponent component = SimpleCommandHandlingComponent.create("component");
        component.subscribe(NAME, replying("own"));
        component.subscribe(sub);

        assertEquals(Set.of(NAME, other), component.supportedCommands());
    }

    @Test
    void componentDuplicateHandlerSubscriptionIsRejected() {
        SimpleCommandHandlingComponent component = SimpleCommandHandlingComponent.create("component");
        component.subscribe(NAME, replying("one"));

        assertThrows(DuplicateCommandHandlerSubscriptionException.class,
                     () -> component.subscribe(NAME, replying("two")));
    }

    @Test
    void componentWithoutHandlerFailsWithNoHandlerForCommandException() {
        SimpleCommandHandlingComponent component = SimpleCommandHandlingComponent.create("component");
        component.subscribe(new QualifiedName("other.command"), replying("other"));

        MessageStream<?> result = component.handle(command(), null);

        NoHandlerForCommandException error =
                assertInstanceOf(NoHandlerForCommandException.class, result.error().orElseThrow());
        assertTrue(error.getMessage().contains("test.command"));
        assertTrue(error.getMessage().contains("other.command"));
    }

    @Test
    void componentConvertsThrownHandlerExceptionsIntoAFailedStream() {
        SimpleCommandHandlingComponent component = SimpleCommandHandlingComponent.create("component");
        component.subscribe(NAME, (command, context) -> {
            throw new IllegalStateException("boom");
        });

        MessageStream<?> result = component.handle(command(), null);

        assertInstanceOf(IllegalStateException.class, result.error().orElseThrow());
    }

    @Test
    void componentRejectsANullCommand() {
        SimpleCommandHandlingComponent component = SimpleCommandHandlingComponent.create("component");

        assertThrows(NullPointerException.class, () -> component.handle(null, null));
    }
}
