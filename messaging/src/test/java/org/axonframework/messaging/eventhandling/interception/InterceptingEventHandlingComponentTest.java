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

package org.axonframework.messaging.eventhandling.interception;

import org.axonframework.common.infra.ComponentDescriptor;
import org.axonframework.messaging.core.EmptyApplicationContext;
import org.axonframework.messaging.core.MessageHandlerInterceptor;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.unitofwork.ProcessingContext;
import org.axonframework.messaging.core.unitofwork.SimpleUnitOfWorkFactory;
import org.axonframework.messaging.eventhandling.EventHandler;
import org.axonframework.messaging.eventhandling.EventMessage;
import org.axonframework.messaging.eventhandling.GenericEventMessage;
import org.axonframework.messaging.eventhandling.SimpleEventHandlingComponent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link InterceptingEventHandlingComponent} and the
 * {@link EventMessageHandlerInterceptorChain}.
 */
class InterceptingEventHandlingComponentTest {

    private static final MessageType TYPE = new MessageType("test.event");

    private final SimpleUnitOfWorkFactory unitOfWorkFactory =
            new SimpleUnitOfWorkFactory(EmptyApplicationContext.INSTANCE);
    private final List<String> calls = new ArrayList<>();

    private static EventMessage event() {
        return new GenericEventMessage(TYPE, "payload");
    }

    private <R> R inContext(Function<ProcessingContext, R> work) {
        return unitOfWorkFactory.create()
                                .executeWithResult(context -> CompletableFuture.completedFuture(work.apply(context)))
                                .join();
    }

    private void handle(InterceptingEventHandlingComponent component) {
        inContext(ctx -> component.handle(event(), ctx).first().asCompletableFuture().join());
    }

    private MessageHandlerInterceptor<EventMessage> recording(String name) {
        return (message, context, chain) -> {
            calls.add(name + ":before");
            MessageStream<?> result = chain.proceed(message, context);
            calls.add(name + ":after");
            return result;
        };
    }

    private EventHandler recordingHandler() {
        return (event, context) -> {
            calls.add("handler");
            return MessageStream.empty();
        };
    }

    private SimpleEventHandlingComponent delegate() {
        SimpleEventHandlingComponent delegate = SimpleEventHandlingComponent.create("delegate");
        delegate.subscribe(TYPE.qualifiedName(), recordingHandler());
        return delegate;
    }

    @Test
    void interceptorsWrapTheDelegateInRegistrationOrder() {
        InterceptingEventHandlingComponent component = new InterceptingEventHandlingComponent(
                List.of(recording("first"), recording("second")), delegate()
        );

        handle(component);

        assertEquals(List.of("first:before", "second:before", "handler", "second:after", "first:after"), calls);
    }

    @Test
    void withoutInterceptorsTheDelegateIsInvokedDirectly() {
        InterceptingEventHandlingComponent component = new InterceptingEventHandlingComponent(List.of(), delegate());

        handle(component);

        assertEquals(List.of("handler"), calls);
    }

    @Test
    void interceptorCanShortCircuitTheChain() {
        MessageHandlerInterceptor<EventMessage> blocking = (message, context, chain) -> {
            calls.add("blocked");
            return MessageStream.empty();
        };
        InterceptingEventHandlingComponent component = new InterceptingEventHandlingComponent(
                List.of(blocking), delegate()
        );

        handle(component);

        assertEquals(List.of("blocked"), calls);
    }

    @Test
    void interceptorFailureSurfacesThroughTheStream() {
        MessageHandlerInterceptor<EventMessage> failing = (message, context, chain) -> MessageStream.failed(
                new IllegalStateException("rejected")
        );
        InterceptingEventHandlingComponent component = new InterceptingEventHandlingComponent(
                List.of(failing), delegate()
        );

        CompletionException failure = assertThrows(CompletionException.class, () -> handle(component));

        assertEquals("rejected", failure.getCause().getMessage());
        assertTrue(calls.isEmpty());
    }

    @Test
    void exceptionThrownByAnInterceptorIsReportedAsAFailedStream() {
        MessageHandlerInterceptor<EventMessage> throwing = (message, context, chain) -> {
            throw new IllegalArgumentException("thrown");
        };
        InterceptingEventHandlingComponent component = new InterceptingEventHandlingComponent(
                List.of(throwing), delegate()
        );

        CompletionException failure = assertThrows(CompletionException.class, () -> handle(component));

        assertEquals("thrown", failure.getCause().getMessage());
    }

    @Test
    void chainProceedInvokesTheInterceptorsAndHandler() {
        EventMessageHandlerInterceptorChain chain = new EventMessageHandlerInterceptorChain(
                List.of(recording("only")), recordingHandler()
        );

        inContext(ctx -> chain.proceed(event(), ctx).first().asCompletableFuture().join());

        assertEquals(List.of("only:before", "handler", "only:after"), calls);
    }

    @Test
    void chainDoesNotObserveLaterChangesToTheInterceptorList() {
        List<MessageHandlerInterceptor<? super EventMessage>> interceptors = new ArrayList<>();
        EventMessageHandlerInterceptorChain chain = new EventMessageHandlerInterceptorChain(
                interceptors, recordingHandler()
        );
        interceptors.add(recording("late"));

        inContext(ctx -> chain.proceed(event(), ctx).first().asCompletableFuture().join());

        assertEquals(List.of("handler"), calls);
    }

    @Test
    void rejectsNullArguments() {
        assertEquals("The Event Handler may not be null.", assertThrows(
                NullPointerException.class, () -> new EventMessageHandlerInterceptorChain(List.of(), null)
        ).getMessage());
        assertEquals("The interceptors must not be null.", assertThrows(
                NullPointerException.class, () -> new InterceptingEventHandlingComponent(null, delegate())
        ).getMessage());
    }

    @Test
    void describesDelegateAndInterceptors() {
        SimpleEventHandlingComponent delegate = delegate();
        MessageHandlerInterceptor<EventMessage> interceptor = recording("x");
        InterceptingEventHandlingComponent component = new InterceptingEventHandlingComponent(
                List.of(interceptor), delegate
        );
        Map<String, Object> properties = new HashMap<>();
        List<Object> wrapped = new ArrayList<>();
        ComponentDescriptor descriptor = (ComponentDescriptor) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{ComponentDescriptor.class}, (proxy, method, args) -> {
                    if (method.getName().equals("describeProperty") && args.length == 2) {
                        properties.put((String) args[0], args[1]);
                    } else if (method.getName().equals("describeWrapperOf")) {
                        wrapped.add(args[0]);
                    }
                    return null;
                }
        );

        component.describeTo(descriptor);

        assertEquals(List.of(delegate), wrapped);
        assertEquals(List.of(interceptor), properties.get("interceptors"));
        assertInstanceOf(List.class, properties.get("interceptors"));
        assertSame(delegate, wrapped.get(0));
    }

    @Test
    void decorationOrderRunsEarly() {
        assertEquals(Integer.MIN_VALUE + 100, InterceptingEventHandlingComponent.DECORATION_ORDER);
    }
}
