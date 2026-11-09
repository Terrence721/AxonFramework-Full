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
package org.axonframework.messaging.commandhandling.tracing;

import org.axonframework.common.infra.ComponentDescriptor;
import org.axonframework.messaging.commandhandling.CommandMessage;
import org.axonframework.messaging.commandhandling.GenericCommandMessage;
import org.axonframework.messaging.commandhandling.GenericCommandResultMessage;
import org.axonframework.messaging.commandhandling.NoHandlerForCommandException;
import org.axonframework.messaging.commandhandling.SimpleCommandBus;
import org.axonframework.messaging.core.EmptyApplicationContext;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.unitofwork.SimpleUnitOfWorkFactory;
import org.axonframework.messaging.tracing.LoggingSpanFactory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test class validating the {@link TracingCommandBus} delegation, using a real {@link SimpleCommandBus} and the
 * {@link LoggingSpanFactory}.
 */
class TracingCommandBusTest {

    private static final MessageType TYPE = new MessageType("test.command");

    private final SimpleCommandBus delegate =
            new SimpleCommandBus(new SimpleUnitOfWorkFactory(EmptyApplicationContext.INSTANCE));
    private final TracingCommandBus testSubject = new TracingCommandBus(delegate, LoggingSpanFactory.INSTANCE);

    private static CommandMessage command() {
        return new GenericCommandMessage(TYPE, "payload");
    }

    @Test
    void constructorRejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> new TracingCommandBus(null, LoggingSpanFactory.INSTANCE));
        assertThrows(NullPointerException.class, () -> new TracingCommandBus(delegate, null));
    }

    @Test
    void subscribeReturnsTheTracingBus() {
        assertSame(testSubject, testSubject.subscribe(TYPE.qualifiedName(), (command, context) ->
                MessageStream.just(new GenericCommandResultMessage(new MessageType("test.result"), "done"))));
    }

    @Test
    void dispatchHandsTheCommandToTheHandlerAndReturnsItsResult() {
        AtomicReference<CommandMessage> received = new AtomicReference<>();
        testSubject.subscribe(TYPE.qualifiedName(), (command, context) -> {
            received.set(command);
            return MessageStream.just(new GenericCommandResultMessage(new MessageType("test.result"), "done"));
        });
        CommandMessage command = command();

        assertEquals("done", testSubject.dispatch(command, null).join().payload());
        assertEquals(command.identifier(), received.get().identifier());
        assertEquals("payload", received.get().payload());
    }

    @Test
    void dispatchWithoutHandlerFailsWithNoHandlerForCommandException() {
        CompletionException failure =
                assertThrows(CompletionException.class, () -> testSubject.dispatch(command(), null).join());

        assertInstanceOf(NoHandlerForCommandException.class, failure.getCause());
    }

    @Test
    void handlerFailuresArePropagated() {
        IllegalStateException error = new IllegalStateException("boom");
        testSubject.subscribe(TYPE.qualifiedName(), (command, context) -> MessageStream.failed(error));

        CompletionException failure =
                assertThrows(CompletionException.class, () -> testSubject.dispatch(command(), null).join());

        assertSame(error, failure.getCause());
    }

    @Test
    void describesItselfAsAWrapperOfTheDelegate() {
        Map<String, Object> properties = new HashMap<>();
        List<Object> wrapped = new ArrayList<>();
        ComponentDescriptor descriptor = (ComponentDescriptor) Proxy.newProxyInstance(
                getClass().getClassLoader(),
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

        testSubject.describeTo(descriptor);

        assertEquals(List.of(delegate), wrapped);
        assertSame(LoggingSpanFactory.INSTANCE, properties.get("spanFactory"));
    }
}
