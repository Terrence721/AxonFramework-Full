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

package org.axonframework.messaging.tracing.annotation;

import org.axonframework.common.Priority;
import org.axonframework.common.configuration.ComponentNotFoundException;
import org.axonframework.common.infra.ComponentDescriptor;
import org.axonframework.messaging.commandhandling.annotation.CommandHandlingMember;
import org.axonframework.messaging.core.ApplicationContext;
import org.axonframework.messaging.core.Message;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.annotation.MessageHandlingMember;
import org.axonframework.messaging.core.unitofwork.ProcessingContext;
import org.axonframework.messaging.eventhandling.GenericEventMessage;
import org.axonframework.messaging.eventhandling.annotation.EventHandlingMember;
import org.axonframework.messaging.queryhandling.annotation.QueryHandlingMember;
import org.axonframework.messaging.tracing.LoggingSpanFactory;
import org.axonframework.messaging.tracing.Span;
import org.axonframework.messaging.tracing.SpanFactory;
import org.axonframework.messaging.tracing.SpanScope;
import org.axonframework.messaging.tracing.configuration.MessagingTracingSettings;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.axonframework.messaging.tracing.attributes.TracingTestContexts.inStartedContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link TracingHandlerEnhancerDefinition}, using fake handler members and an application
 * context that decides which components the handler can resolve.
 */
class TracingHandlerEnhancerDefinitionTest {

    private static final Message MESSAGE = new GenericEventMessage(new MessageType("test.event"), "payload");

    private final TracingHandlerEnhancerDefinition testSubject = new TracingHandlerEnhancerDefinition();
    private final RecordingSpanFactory spanFactory = new RecordingSpanFactory();

    /**
     * A target type, whose simple name shows up in span names, with a method to take a signature from.
     */
    static class Orders {

        @SuppressWarnings("java:S119")
        void on(String payload, int version) {
        }
    }

    private static Method handlerMethod() throws NoSuchMethodException {
        return Orders.class.getDeclaredMethod("on", String.class, int.class);
    }

    // --- wrapping ---

    @Test
    void membersWithoutAnExecutableAreLeftAlone() {
        FakeMember member = new FakeMember(null);

        assertSame(member, testSubject.wrapHandler(member));
    }

    @Test
    void aPlainMemberIsWrappedAndKeepsDelegatingItsProperties() throws Exception {
        FakeMember member = new FakeMember(handlerMethod());

        MessageHandlingMember<Object> wrapped = testSubject.wrapHandler(member);

        assertNotSame(member, wrapped);
        assertEquals(Object.class, wrapped.payloadType());
        assertTrue(wrapped.canHandleMessageType(Message.class));
        assertFalse(wrapped instanceof CommandHandlingMember);
        assertFalse(wrapped instanceof EventHandlingMember);
        assertFalse(wrapped instanceof QueryHandlingMember);
    }

    @Test
    void aCommandHandlingMemberStaysACommandHandlingMemberWithItsProperties() throws Exception {
        FakeCommandMember member = new FakeCommandMember(handlerMethod());

        MessageHandlingMember<Object> wrapped = testSubject.wrapHandler(member);

        CommandHandlingMember<?> command = assertInstanceOf(CommandHandlingMember.class, wrapped);
        assertEquals("create-order", command.commandName());
        assertEquals("orderId", command.routingKey());
        assertTrue(command.isFactoryHandler());
    }

    @Test
    void anEventHandlingMemberStaysAnEventHandlingMemberWithItsProperties() throws Exception {
        FakeEventMember member = new FakeEventMember(handlerMethod());

        MessageHandlingMember<Object> wrapped = testSubject.wrapHandler(member);

        assertEquals("order-created", assertInstanceOf(EventHandlingMember.class, wrapped).eventName());
    }

    @Test
    void aQueryHandlingMemberStaysAQueryHandlingMemberWithItsProperties() throws Exception {
        FakeQueryMember member = new FakeQueryMember(handlerMethod());

        MessageHandlingMember<Object> wrapped = testSubject.wrapHandler(member);

        QueryHandlingMember<?> query = assertInstanceOf(QueryHandlingMember.class, wrapped);
        assertEquals("find-order", query.queryName());
        assertEquals(String.class, query.resultType());
    }

    @Test
    void runsAsTheLastHandlerEnhancer() {
        assertEquals(Priority.LAST, TracingHandlerEnhancerDefinition.class.getAnnotation(Priority.class).value());
    }

    // --- handling ---

    @Test
    void handlingWithoutAnApplicationContextFallsBackToAPassThrough() throws Exception {
        FakeMember member = new FakeMember(handlerMethod());
        MessageHandlingMember<Object> wrapped = testSubject.wrapHandler(member);

        MessageStream<?> result = inStartedContext(context -> wrapped.handle(MESSAGE, context, new Orders()));

        assertSame(member.result, result);
        assertEquals(1, member.handleCalls);
        assertTrue(spanFactory.names.isEmpty());
    }

    @Test
    void handlingWithoutASpanFactoryFallsBackToAPassThrough() throws Exception {
        FakeMember member = new FakeMember(handlerMethod());
        MessageHandlingMember<Object> wrapped = testSubject.wrapHandler(member);
        TestApplicationContext applicationContext = new TestApplicationContext();

        MessageStream<?> result =
                inStartedContext(applicationContext, context -> wrapped.handle(MESSAGE, context, new Orders()));

        assertSame(member.result, result);
        assertEquals(List.of(SpanFactory.class), applicationContext.requested);
    }

    @Test
    void handlingWithASpanFactoryRunsTheMethodInANamedInternalSpanWithItsScopeInTheContext() throws Exception {
        FakeMember member = new FakeMember(handlerMethod());
        MessageHandlingMember<Object> wrapped = testSubject.wrapHandler(member);
        TestApplicationContext applicationContext = new TestApplicationContext().with(SpanFactory.class, spanFactory);
        AtomicReference<SpanScope> scopeSeenByTheMethod = new AtomicReference<>();
        member.onHandle = context -> scopeSeenByTheMethod.set(SpanScope.fromContext(context));

        inStartedContext(applicationContext, context -> {
            MessageStream<?> result = wrapped.handle(MESSAGE, context, new Orders());
            while (result.next().isPresent()) {
                // drain the stream, completing the span
            }
            return result;
        });

        assertEquals(List.of("Orders.on(String,int)"), spanFactory.names);
        assertEquals(1, member.handleCalls);
        assertNotNull(scopeSeenByTheMethod.get(), "The method did not run with the span scope in its context");
        assertTrue(scopeSeenByTheMethod.get().isClosed(), "The span was not closed when the stream ended");
    }

    @Test
    void theSpanOfAStaticMethodUsesJustTheSignature() throws Exception {
        MessageHandlingMember<Object> wrapped = testSubject.wrapHandler(new FakeMember(handlerMethod()));
        TestApplicationContext applicationContext = new TestApplicationContext().with(SpanFactory.class, spanFactory);

        inStartedContext(applicationContext, context -> wrapped.handle(MESSAGE, context, null));

        assertEquals(List.of("on(String,int)"), spanFactory.names);
    }

    @Test
    void suppressedEventSourcingHandlersSkipTheSpanFactoryEntirely() throws Exception {
        FakeMember member = new FakeMember(handlerMethod());
        member.eventSourcingHandler = true;
        MessageHandlingMember<Object> wrapped = testSubject.wrapHandler(member);
        TestApplicationContext applicationContext = new TestApplicationContext()
                .with(SpanFactory.class, spanFactory)
                .with(MessagingTracingSettings.class, MessagingTracingSettings.enabledByDefault());

        MessageStream<?> result =
                inStartedContext(applicationContext, context -> wrapped.handle(MESSAGE, context, new Orders()));

        assertSame(member.result, result);
        assertTrue(spanFactory.names.isEmpty());
        assertFalse(applicationContext.requested.contains(SpanFactory.class),
                    "The span factory was resolved for a suppressed event sourcing handler");
    }

    @Test
    void eventSourcingHandlersStaySuppressedWithoutSettings() throws Exception {
        FakeMember member = new FakeMember(handlerMethod());
        member.eventSourcingHandler = true;
        MessageHandlingMember<Object> wrapped = testSubject.wrapHandler(member);
        TestApplicationContext applicationContext = new TestApplicationContext().with(SpanFactory.class, spanFactory);

        inStartedContext(applicationContext, context -> wrapped.handle(MESSAGE, context, new Orders()));

        assertTrue(spanFactory.names.isEmpty());
    }

    @Test
    void eventSourcingHandlersAreTracedWhenTheSettingsEnableIt() throws Exception {
        FakeMember member = new FakeMember(handlerMethod());
        member.eventSourcingHandler = true;
        MessageHandlingMember<Object> wrapped = testSubject.wrapHandler(member);
        TestApplicationContext applicationContext = new TestApplicationContext()
                .with(SpanFactory.class, spanFactory)
                .with(MessagingTracingSettings.class,
                      MessagingTracingSettings.enabledByDefault().withEventSourcingHandlersEnabled(true));

        inStartedContext(applicationContext, context -> wrapped.handle(MESSAGE, context, new Orders()));

        assertEquals(List.of("Orders.on(String,int)"), spanFactory.names);
    }

    @Test
    void anEventSourcingEventHandlerKeepsItsKindWhenWrapped() throws Exception {
        FakeEventMember member = new FakeEventMember(handlerMethod());
        member.eventSourcingHandler = true;

        MessageHandlingMember<Object> wrapped = testSubject.wrapHandler(member);

        assertInstanceOf(EventHandlingMember.class, wrapped);
    }

    // --- test doubles ---

    private static class FakeMember implements MessageHandlingMember<Object> {

        private final Executable executable;
        final MessageStream<Message> result = MessageStream.empty().cast();
        boolean eventSourcingHandler;
        int handleCalls;
        Consumer<ProcessingContext> onHandle = context -> {
        };

        private FakeMember(@Nullable Executable executable) {
            this.executable = executable;
        }

        @Override
        public Class<?> payloadType() {
            return Object.class;
        }

        @Override
        public boolean canHandle(Message message, ProcessingContext context) {
            return true;
        }

        @Override
        public boolean canHandleMessageType(Class<? extends Message> messageType) {
            return true;
        }

        @Override
        public MessageStream<?> handle(Message message, ProcessingContext context, @Nullable Object target) {
            handleCalls++;
            onHandle.accept(context);
            return result;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <HT> Optional<HT> unwrap(Class<HT> handlerType) {
            if (handlerType == Executable.class) {
                return Optional.ofNullable((HT) executable);
            }
            return handlerType.isInstance(this) ? Optional.of(handlerType.cast(this)) : Optional.empty();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <R> Optional<R> attribute(String attributeKey) {
            return eventSourcingHandler && "EventSourcingHandler.payloadType".equals(attributeKey)
                    ? Optional.of((R) Object.class)
                    : Optional.empty();
        }
    }

    private static final class FakeCommandMember extends FakeMember implements CommandHandlingMember<Object> {

        private FakeCommandMember(Executable executable) {
            super(executable);
        }

        @Override
        public String commandName() {
            return "create-order";
        }

        @Override
        public String routingKey() {
            return "orderId";
        }

        @Override
        public boolean isFactoryHandler() {
            return true;
        }
    }

    private static final class FakeEventMember extends FakeMember implements EventHandlingMember<Object> {

        private FakeEventMember(Executable executable) {
            super(executable);
        }

        @Override
        public String eventName() {
            return "order-created";
        }
    }

    private static final class FakeQueryMember extends FakeMember implements QueryHandlingMember<Object> {

        private FakeQueryMember(Executable executable) {
            super(executable);
        }

        @Override
        public String queryName() {
            return "find-order";
        }

        @Override
        public Type resultType() {
            return String.class;
        }
    }

    /**
     * Resolves only the components it was given and remembers which types were asked for.
     */
    private static final class TestApplicationContext implements ApplicationContext {

        private final Map<Class<?>, Object> components = new HashMap<>();
        private final List<Class<?>> requested = new ArrayList<>();

        private <C> TestApplicationContext with(Class<C> type, C component) {
            components.put(type, component);
            return this;
        }

        @Override
        public <C> C component(Class<C> type, @Nullable String name) {
            requested.add(type);
            Object component = components.get(type);
            if (component == null) {
                throw new ComponentNotFoundException(type, name);
            }
            return type.cast(component);
        }
    }

    /**
     * A span factory that remembers the names of the internal spans it was asked for, delegating to the logging one.
     */
    private static final class RecordingSpanFactory implements SpanFactory {

        private final List<String> names = new ArrayList<>();

        @Override
        public Span createInternalSpan(String operationName, @Nullable ProcessingContext context) {
            names.add(operationName);
            return LoggingSpanFactory.INSTANCE.createInternalSpan(operationName, context);
        }

        @Override
        public Span createDispatchSpan(String operationName, Message message, @Nullable ProcessingContext context) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Span createHandlerSpan(String operationName, Message message, @Nullable ProcessingContext context) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Span createContextParentHandlerSpan(String operationName, Message message,
                                                   @Nullable ProcessingContext context) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Span createLinkedHandlerSpan(String operationName, Message message, Message linkedMessage,
                                            @Nullable ProcessingContext context) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Span createDisconnectedHandlerSpan(String operationName, Message message,
                                                  @Nullable ProcessingContext context) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Span createRootSpan(String operationName, @Nullable ProcessingContext context) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void describeTo(ComponentDescriptor descriptor) {
        }
    }
}
