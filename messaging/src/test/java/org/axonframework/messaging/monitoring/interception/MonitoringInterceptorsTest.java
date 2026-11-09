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
 * See the License for the specific language governing permissions and limitations
 * under the License.
 */

package org.axonframework.messaging.monitoring.interception;

import org.axonframework.messaging.commandhandling.CommandMessage;
import org.axonframework.messaging.commandhandling.GenericCommandMessage;
import org.axonframework.messaging.core.ApplicationContext;
import org.axonframework.messaging.core.Message;
import org.axonframework.messaging.core.MessageDispatchInterceptorChain;
import org.axonframework.messaging.core.MessageHandlerInterceptorChain;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.unitofwork.ProcessingContext;
import org.axonframework.messaging.core.unitofwork.SimpleUnitOfWorkFactory;
import org.axonframework.messaging.core.unitofwork.UnitOfWork;
import org.axonframework.messaging.eventhandling.EventMessage;
import org.axonframework.messaging.eventhandling.GenericEventMessage;
import org.axonframework.messaging.monitoring.MessageMonitor;
import org.axonframework.messaging.monitoring.RecordingMessageMonitor;
import org.axonframework.messaging.queryhandling.GenericQueryMessage;
import org.axonframework.messaging.queryhandling.GenericSubscriptionQueryUpdateMessage;
import org.axonframework.messaging.queryhandling.QueryMessage;
import org.axonframework.messaging.queryhandling.SubscriptionQueryUpdateMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the five monitoring interceptors: the command, event and query handler interceptors and the
 * event and subscription query update dispatch interceptors. They all behave the same way, so they are tested together.
 */
class MonitoringInterceptorsTest {

    private final ApplicationContext applicationContext = new ApplicationContext() {
        @Override
        public <C> C component(Class<C> type, String name) {
            throw new UnsupportedOperationException();
        }
    };
    private final RecordingMessageMonitor monitor = new RecordingMessageMonitor("m");
    private final RecordingChain chain = new RecordingChain();

    @FunctionalInterface
    private interface Invocation {

        MessageStream<?> invoke(MessageMonitor<Message> monitor,
                                Message message,
                                ProcessingContext context,
                                RecordingChain chain);
    }

    private record Scenario(String name, Message message, Invocation invocation) {

        @Override
        public String toString() {
            return name;
        }
    }

    static Stream<Scenario> scenarios() {
        return Stream.of(
                new Scenario("command handler interceptor",
                             new GenericCommandMessage(new MessageType("test.command"), "payload"),
                             (monitor, message, context, chain) -> new MonitoringCommandHandlerInterceptor(monitor)
                                     .interceptOnHandle((CommandMessage) message, context, chain.handlerChain())),
                new Scenario("event handler interceptor",
                             new GenericEventMessage(new MessageType("test.event"), "payload"),
                             (monitor, message, context, chain) -> new MonitoringEventHandlerInterceptor(monitor)
                                     .interceptOnHandle((EventMessage) message, context, chain.handlerChain())),
                new Scenario("query handler interceptor",
                             new GenericQueryMessage(new MessageType("test.query"), "payload"),
                             (monitor, message, context, chain) -> new MonitoringQueryHandlerInterceptor(monitor)
                                     .interceptOnHandle((QueryMessage) message, context, chain.handlerChain())),
                new Scenario("event dispatch interceptor",
                             new GenericEventMessage(new MessageType("test.event"), "payload"),
                             (monitor, message, context, chain) -> new MonitoringEventDispatchInterceptor(monitor)
                                     .interceptOnDispatch((EventMessage) message, context, chain.dispatchChain())),
                new Scenario("subscription query update dispatch interceptor",
                             new GenericSubscriptionQueryUpdateMessage(new MessageType("test.update"), "payload"),
                             (monitor, message, context, chain) ->
                                     new MonitoringSubscriptionQueryUpdateDispatchInterceptor(monitor)
                                             .interceptOnDispatch((SubscriptionQueryUpdateMessage) message, context,
                                                                  chain.dispatchChain()))
        );
    }

    private UnitOfWork unitOfWork() {
        return new SimpleUnitOfWorkFactory(applicationContext).create();
    }

    private static ProcessingContext notStartedContext() {
        return (ProcessingContext) Proxy.newProxyInstance(
                MonitoringInterceptorsTest.class.getClassLoader(), new Class<?>[]{ProcessingContext.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "isStarted":
                            return false;
                        case "equals":
                            return proxy == args[0];
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void reportsSuccessAfterTheUnitOfWorkCommitted(Scenario scenario) {
        unitOfWork().executeWithResult(context -> {
            scenario.invocation().invoke(monitor, scenario.message(), context, chain);
            // Nothing is reported as a result yet while the unit of work is still running
            assertEquals(List.of("m:ingested:" + scenario.message().identifier()), monitor.events());
            return CompletableFuture.completedFuture("done");
        }).join();

        assertEquals(List.of("m:ingested:" + scenario.message().identifier(), "m:success"), monitor.events());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void reportsFailureWhenTheUnitOfWorkFails(Scenario scenario) {
        CompletableFuture<Object> failing = unitOfWork().executeWithResult(context -> {
            scenario.invocation().invoke(monitor, scenario.message(), context, chain);
            return CompletableFuture.failedFuture(new IllegalStateException("boom"));
        });

        assertThrows(CompletionException.class, failing::join);
        assertEquals(2, monitor.events().size());
        assertEquals("m:ingested:" + scenario.message().identifier(), monitor.events().get(0));
        assertTrue(monitor.events().get(1).startsWith("m:failure:") && monitor.events().get(1).contains("boom"),
                   monitor.events().toString());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void alwaysProceedsWithTheSameMessageAndContextAndReturnsTheChainResult(Scenario scenario) {
        unitOfWork().executeWithResult(context -> {
            MessageStream<?> result = scenario.invocation().invoke(monitor, scenario.message(), context, chain);

            assertSame(chain.result, result);
            assertEquals(1, chain.calls);
            assertSame(scenario.message(), chain.lastMessage);
            assertSame(context, chain.lastContext);
            return CompletableFuture.completedFuture("done");
        }).join();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void doesNotMonitorWhenTheContextIsNotStarted(Scenario scenario) {
        ProcessingContext context = notStartedContext();

        MessageStream<?> result = scenario.invocation().invoke(monitor, scenario.message(), context, chain);

        assertTrue(monitor.events().isEmpty());
        assertSame(chain.result, result);
        assertEquals(1, chain.calls);
        assertSame(context, chain.lastContext);
    }

    @Test
    void dispatchInterceptorsProceedWithoutMonitoringWhenThereIsNoContext() {
        Message event = new GenericEventMessage(new MessageType("test.event"), "payload");
        Message update = new GenericSubscriptionQueryUpdateMessage(new MessageType("test.update"), "payload");

        new MonitoringEventDispatchInterceptor(monitor)
                .interceptOnDispatch((EventMessage) event, null, chain.dispatchChain());
        new MonitoringSubscriptionQueryUpdateDispatchInterceptor(monitor)
                .interceptOnDispatch((SubscriptionQueryUpdateMessage) update, null, chain.dispatchChain());

        assertTrue(monitor.events().isEmpty());
        assertEquals(2, chain.calls);
    }

    /**
     * Records how the interceptor chains were proceeded with.
     */
    private static final class RecordingChain {

        private final MessageStream<Message> result = MessageStream.empty().cast();
        private int calls;
        private Message lastMessage;
        private ProcessingContext lastContext;

        <M extends Message> MessageHandlerInterceptorChain<M> handlerChain() {
            return (message, context) -> {
                calls++;
                lastMessage = message;
                lastContext = context;
                return result;
            };
        }

        <M extends Message> MessageDispatchInterceptorChain<M> dispatchChain() {
            return (message, context) -> {
                calls++;
                lastMessage = message;
                lastContext = context;
                return result;
            };
        }
    }
}
