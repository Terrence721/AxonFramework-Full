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
import org.axonframework.conversion.ConversionException;
import org.axonframework.messaging.commandhandling.CommandExecutionException;
import org.axonframework.messaging.commandhandling.CommandHandler;
import org.axonframework.messaging.commandhandling.CommandMessage;
import org.axonframework.messaging.commandhandling.CommandPriorityCalculator;
import org.axonframework.messaging.commandhandling.GenericCommandMessage;
import org.axonframework.messaging.commandhandling.GenericCommandResultMessage;
import org.axonframework.messaging.commandhandling.NoHandlerForCommandException;
import org.axonframework.messaging.commandhandling.RoutingStrategy;
import org.axonframework.messaging.commandhandling.SimpleCommandBus;
import org.axonframework.messaging.core.ClassBasedMessageTypeResolver;
import org.axonframework.messaging.core.EmptyApplicationContext;
import org.axonframework.messaging.core.GenericMessage;
import org.axonframework.messaging.core.Message;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.MessageTypeResolver;
import org.axonframework.messaging.core.Metadata;
import org.axonframework.messaging.core.QualifiedName;
import org.axonframework.messaging.core.unitofwork.SimpleUnitOfWorkFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test class validating the {@link DefaultCommandGateway}, {@link CommandResult} and {@link FutureCommandResult}.
 */
class DefaultCommandGatewayTest {

    private static final MessageType RESULT_TYPE = new MessageType("test.result");

    private final AtomicReference<CommandMessage> received = new AtomicReference<>();
    private final AtomicInteger routingCalls = new AtomicInteger();
    private final AtomicInteger priorityCalls = new AtomicInteger();
    private final MessageTypeResolver typeResolver = new ClassBasedMessageTypeResolver();
    private final RoutingStrategy routingStrategy = command -> {
        routingCalls.incrementAndGet();
        return "resolved-routing";
    };
    private final CommandPriorityCalculator priorityCalculator = command -> {
        priorityCalls.incrementAndGet();
        return 7;
    };

    private SimpleCommandBus commandBus;
    private DefaultCommandGateway gateway;

    @BeforeEach
    void setUp() {
        commandBus = new SimpleCommandBus(new SimpleUnitOfWorkFactory(EmptyApplicationContext.INSTANCE));
        commandBus.subscribe(new QualifiedName(String.class), echoing());
        gateway = new DefaultCommandGateway(commandBus, typeResolver, priorityCalculator, routingStrategy);
    }

    private CommandHandler echoing() {
        return (command, context) -> {
            received.set(command);
            return MessageStream.just(new GenericCommandResultMessage(RESULT_TYPE, "result:" + command.payload()));
        };
    }

    private DefaultCommandGateway gatewayFailingWith(Throwable error) {
        SimpleCommandBus failingBus = new SimpleCommandBus(new SimpleUnitOfWorkFactory(EmptyApplicationContext.INSTANCE));
        failingBus.subscribe(new QualifiedName(String.class), (command, context) -> MessageStream.failed(error));
        return new DefaultCommandGateway(failingBus, typeResolver, priorityCalculator, routingStrategy);
    }

    private static CommandMessage commandWithRoutingAndPriority() {
        return new GenericCommandMessage(new MessageType(String.class), "hello", Metadata.emptyInstance(), "mine", 3);
    }

    @Test
    void constructorRejectsNullArguments() {
        assertThrows(NullPointerException.class,
                     () -> new DefaultCommandGateway(null, typeResolver, priorityCalculator, routingStrategy));
        assertThrows(NullPointerException.class,
                     () -> new DefaultCommandGateway(commandBus, null, priorityCalculator, routingStrategy));
        assertThrows(NullPointerException.class,
                     () -> new DefaultCommandGateway(commandBus, typeResolver, null, routingStrategy));
        assertThrows(NullPointerException.class,
                     () -> new DefaultCommandGateway(commandBus, typeResolver, priorityCalculator, null));
    }

    @Test
    void sendsAPlainPayloadWrappedInACommandMessageWithResolvedTypeMetadataRoutingAndPriority() {
        String result = gateway.send("hello", Metadata.with("k", "v"), null).resultAs(String.class).join();

        assertEquals("result:hello", result);
        CommandMessage command = received.get();
        assertEquals("hello", command.payload());
        assertEquals(new QualifiedName(String.class), command.type().qualifiedName());
        assertEquals("v", command.metadata().get("k"));
        assertEquals(Optional.of("resolved-routing"), command.routingKey());
        assertEquals(OptionalInt.of(7), command.priority());
    }

    @Test
    void sendsACommandMessageKeepingItsIdentifier() {
        CommandMessage message = new GenericCommandMessage(new MessageType(String.class), "hello");

        gateway.send(message, Metadata.emptyInstance(), null).getResultMessage().join();

        assertEquals(message.identifier(), received.get().identifier());
    }

    @Test
    void keepsRoutingKeyAndPriorityAlreadyPresentOnTheCommandMessage() {
        gateway.send(commandWithRoutingAndPriority(), Metadata.emptyInstance(), null).getResultMessage().join();

        assertEquals(Optional.of("mine"), received.get().routingKey());
        assertEquals(OptionalInt.of(3), received.get().priority());
    }

    @Test
    void doesNotInvokeStrategyOrCalculatorWhenRoutingKeyAndPriorityArePresent() {
        gateway.send(commandWithRoutingAndPriority(), Metadata.emptyInstance(), null).getResultMessage().join();

        assertEquals(0, routingCalls.get());
        assertEquals(0, priorityCalls.get());
    }

    @Test
    void aThrowingRoutingStrategyDoesNotAffectCommandsThatAlreadyHaveARoutingKey() {
        DefaultCommandGateway strict = new DefaultCommandGateway(commandBus, typeResolver, priorityCalculator, command -> {
            throw new IllegalStateException("no routing key expected");
        });

        strict.send(commandWithRoutingAndPriority(), Metadata.emptyInstance(), null).getResultMessage().join();

        assertEquals(Optional.of("mine"), received.get().routingKey());
    }

    @Test
    void otherMessageImplementationsAreConvertedUsingTheirTypePayloadAndMetadata() {
        GenericMessage message = new GenericMessage(new MessageType(String.class), "hello", Metadata.with("m", "1"));

        gateway.send(message, Metadata.emptyInstance(), null).getResultMessage().join();

        CommandMessage command = received.get();
        assertEquals("hello", command.payload());
        assertEquals("1", command.metadata().get("m"));
        assertEquals(Optional.of("resolved-routing"), command.routingKey());
    }

    @Test
    void sendWithoutHandlerFailsWithNoHandlerForCommandException() {
        CompletableFuture<Integer> future = gateway.send(42, Integer.class, Metadata.emptyInstance(), null);

        Throwable cause = assertThrows(CompletionException.class, future::join).getCause();

        assertInstanceOf(NoHandlerForCommandException.class, cause);
    }

    @Test
    void sendAndWaitReturnsTheResult() {
        assertEquals("result:hi", gateway.sendAndWait("hi", String.class));
    }

    @Test
    void sendAndWaitRethrowsRuntimeExceptionsUnwrapped() {
        IllegalStateException error = new IllegalStateException("boom");
        DefaultCommandGateway failing = gatewayFailingWith(error);

        assertSame(error, assertThrows(IllegalStateException.class, () -> failing.sendAndWait("hi", String.class)));
    }

    @Test
    void sendAndWaitWrapsCheckedExceptionsInCommandExecutionException() {
        Exception error = new Exception("checked");
        DefaultCommandGateway failing = gatewayFailingWith(error);

        CommandExecutionException thrown =
                assertThrows(CommandExecutionException.class, () -> failing.sendAndWait("hi", String.class));

        assertSame(error, thrown.getCause());
    }

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

        gateway.describeTo(descriptor);

        assertSame(commandBus, properties.get("commandBus"));
        assertSame(typeResolver, properties.get("messageTypeResolver"));
        assertSame(priorityCalculator, properties.get("priorityCalculator"));
        assertSame(routingStrategy, properties.get("routingKeyResolver"));
    }

    // --- CommandResult / FutureCommandResult ---

    private static GenericCommandResultMessage result(Object payload) {
        return new GenericCommandResultMessage(RESULT_TYPE, payload);
    }

    @Test
    void futureCommandResultRejectsANullFuture() {
        assertThrows(NullPointerException.class, () -> new FutureCommandResult(null));
    }

    @Test
    void futureCommandResultExposesTheWrappedFuture() {
        CompletableFuture<GenericCommandResultMessage> future = CompletableFuture.completedFuture(result("x"));

        assertSame(future, new FutureCommandResult(future).getResultMessage());
    }

    @Test
    void resultAsReturnsNullForAMissingMessageOrPayload() {
        assertNull(new FutureCommandResult(CompletableFuture.completedFuture(null)).resultAs(String.class).join());
        assertNull(new FutureCommandResult(CompletableFuture.completedFuture(result(null)))
                           .resultAs(String.class).join());
    }

    @Test
    void resultAsFailsWithConversionExceptionForAnIncompatibleType() {
        CommandResult commandResult = new FutureCommandResult(CompletableFuture.completedFuture(result("text")));

        Throwable cause = assertThrows(CompletionException.class,
                                       () -> commandResult.resultAs(Integer.class).join()).getCause();

        assertInstanceOf(ConversionException.class, cause);
    }

    @Test
    void onSuccessAndOnErrorOnlyFireForTheirOutcome() {
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger errors = new AtomicInteger();

        new FutureCommandResult(CompletableFuture.completedFuture(result("ok")))
                .onSuccess(m -> successes.incrementAndGet())
                .onError(e -> errors.incrementAndGet());
        new FutureCommandResult(CompletableFuture.<GenericCommandResultMessage>failedFuture(new RuntimeException()))
                .onSuccess(m -> successes.incrementAndGet())
                .onError(e -> errors.incrementAndGet());

        assertEquals(1, successes.get());
        assertEquals(1, errors.get());
    }

    @Test
    void typedOnSuccessReceivesPayloadAndMessage() {
        AtomicReference<Object> payload = new AtomicReference<>();
        AtomicReference<Object> message = new AtomicReference<>();
        GenericCommandResultMessage resultMessage = result("ok");

        new FutureCommandResult(CompletableFuture.completedFuture(resultMessage))
                .onSuccess(String.class, (p, m) -> {
                    payload.set(p);
                    message.set(m);
                });

        assertEquals("ok", payload.get());
        assertSame(resultMessage, message.get());
    }

    @Test
    void typedOnSuccessIsInvokedWithNullWhenCommandHandlingYieldsNoMessage() {
        AtomicInteger invocations = new AtomicInteger();
        AtomicReference<Object> payload = new AtomicReference<>("sentinel");

        new FutureCommandResult(CompletableFuture.completedFuture(null))
                .onSuccess(String.class, (String p) -> {
                    invocations.incrementAndGet();
                    payload.set(p);
                });

        assertEquals(1, invocations.get());
        assertNull(payload.get());
    }

    @Test
    void waitReturnsTheTypedPayload() {
        assertEquals("ok", new FutureCommandResult(CompletableFuture.completedFuture(result("ok"))).wait(String.class));
    }

    @Test
    void resultHandlersRejectNullArguments() {
        CommandResult commandResult = new FutureCommandResult(CompletableFuture.completedFuture(result("ok")));

        assertThrows(NullPointerException.class, () -> commandResult.resultAs(null));
        assertThrows(NullPointerException.class, () -> commandResult.onSuccess((Consumer<Message>) null));
        assertThrows(NullPointerException.class, () -> commandResult.onError(null));
    }
}
