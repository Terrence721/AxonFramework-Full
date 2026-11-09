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
package org.axonframework.messaging.commandhandling.retry;

import org.axonframework.common.infra.ComponentDescriptor;
import org.axonframework.messaging.commandhandling.CommandMessage;
import org.axonframework.messaging.commandhandling.GenericCommandMessage;
import org.axonframework.messaging.commandhandling.GenericCommandResultMessage;
import org.axonframework.messaging.commandhandling.NoHandlerForCommandException;
import org.axonframework.messaging.commandhandling.SimpleCommandBus;
import org.axonframework.messaging.core.EmptyApplicationContext;
import org.axonframework.messaging.core.Message;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.retry.RetryScheduler;
import org.axonframework.messaging.core.unitofwork.ProcessingContext;
import org.axonframework.messaging.core.unitofwork.SimpleUnitOfWorkFactory;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test class validating the {@link RetryingCommandBus}.
 */
class RetryingCommandBusTest {

    private static final MessageType TYPE = new MessageType("test.command");

    private final SimpleCommandBus delegate =
            new SimpleCommandBus(new SimpleUnitOfWorkFactory(EmptyApplicationContext.INSTANCE));
    private final List<Throwable> causes = new ArrayList<>();

    /**
     * Retries once by dispatching again, otherwise fails with the cause.
     */
    private final RetryScheduler retryOnce = new RetryScheduler() {
        @Override
        public <M extends Message, R extends Message> MessageStream<R> scheduleRetry(
                M message, @Nullable ProcessingContext context, Throwable cause, Dispatcher<M, R> dispatcher) {
            causes.add(cause);
            return causes.size() == 1 ? dispatcher.dispatch(message, context) : MessageStream.failed(cause);
        }
    };

    private static CommandMessage command() {
        return new GenericCommandMessage(TYPE, "payload");
    }

    @Test
    void constructorRejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> new RetryingCommandBus(null, retryOnce));
        assertThrows(NullPointerException.class, () -> new RetryingCommandBus(delegate, null));
    }

    @Test
    void successfulDispatchIsNotRetried() {
        RetryingCommandBus bus = new RetryingCommandBus(delegate, retryOnce);
        bus.subscribe(TYPE.qualifiedName(), (command, context) -> MessageStream.just(
                new GenericCommandResultMessage(new MessageType("test.result"), "done")));

        assertEquals("done", bus.dispatch(command(), null).join().payload());
        assertEquals(List.of(), causes);
    }

    @Test
    void subscribeReturnsTheRetryingBus() {
        RetryingCommandBus bus = new RetryingCommandBus(delegate, retryOnce);

        assertSame(bus, bus.subscribe(TYPE.qualifiedName(), (command, context) -> MessageStream.just(
                new GenericCommandResultMessage(new MessageType("test.result"), "done"))));
    }

    @Test
    void failedDispatchIsRetriedWithTheUnwrappedCause() {
        AtomicInteger attempts = new AtomicInteger();
        RetryingCommandBus bus = new RetryingCommandBus(delegate, retryOnce);
        bus.subscribe(TYPE.qualifiedName(), (command, context) -> {
            if (attempts.incrementAndGet() == 1) {
                return MessageStream.failed(new IllegalStateException("first"));
            }
            return MessageStream.just(new GenericCommandResultMessage(new MessageType("test.result"), "second"));
        });

        assertEquals("second", bus.dispatch(command(), null).join().payload());
        assertEquals(2, attempts.get());
        assertEquals(1, causes.size());
        assertInstanceOf(IllegalStateException.class, causes.get(0));
    }

    @Test
    void failureOfTheRetryIsPropagated() {
        IllegalStateException error = new IllegalStateException("always");
        RetryingCommandBus bus = new RetryingCommandBus(delegate, retryOnce);
        bus.subscribe(TYPE.qualifiedName(), (command, context) -> MessageStream.failed(error));

        CompletionException failure = assertThrows(CompletionException.class, () -> bus.dispatch(command(), null).join());

        assertSame(error, failure.getCause());
        assertEquals(1, causes.size());
    }

    @Test
    void dispatchWithoutHandlerIsHandedToTheSchedulerAsNoHandlerForCommand() {
        RetryScheduler failing = new RetryScheduler() {
            @Override
            public <M extends Message, R extends Message> MessageStream<R> scheduleRetry(
                    M message, @Nullable ProcessingContext context, Throwable cause, Dispatcher<M, R> dispatcher) {
                causes.add(cause);
                return MessageStream.failed(cause);
            }
        };

        CompletionException failure = assertThrows(
                CompletionException.class,
                () -> new RetryingCommandBus(delegate, failing).dispatch(command(), null).join()
        );

        assertInstanceOf(NoHandlerForCommandException.class, failure.getCause());
        assertInstanceOf(NoHandlerForCommandException.class, causes.get(0));
    }

    @Test
    void describesItself() {
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

        new RetryingCommandBus(delegate, retryOnce).describeTo(descriptor);

        assertEquals(List.of(delegate), wrapped);
        assertSame(retryOnce, properties.get("retryScheduler"));
    }
}
