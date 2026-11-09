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
package org.axonframework.messaging.eventhandling;

import org.axonframework.common.FutureUtils;
import org.axonframework.common.infra.ComponentDescriptor;
import org.axonframework.messaging.core.MessageDispatchInterceptor;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.MessageType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link InterceptingEventBus}, the {@link InterceptingEventSink} and the
 * {@link DelegatingEventBus}.
 */
class InterceptingEventBusTest {

    private static final MessageType TYPE = new MessageType("test.event");

    private final SimpleEventBus delegate = new SimpleEventBus();
    private final List<List<? extends EventMessage>> delivered = new ArrayList<>();

    private static EventMessage event(String payload) {
        return new GenericEventMessage(TYPE, payload);
    }

    private static List<MessageDispatchInterceptor<? super EventMessage>> interceptors(
            MessageDispatchInterceptor<? super EventMessage>... interceptors
    ) {
        return new ArrayList<>(List.of(interceptors));
    }

    private void recordDeliveries() {
        delegate.subscribe((events, context) -> {
            delivered.add(events);
            return FutureUtils.emptyCompletedFuture();
        });
    }

    private static ComponentDescriptor recordingDescriptor(Map<String, Object> properties, List<Object> wrapped) {
        return (ComponentDescriptor) Proxy.newProxyInstance(
                InterceptingEventBusTest.class.getClassLoader(),
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
    void constructorsRejectNullArguments() {
        assertThrows(NullPointerException.class, () -> new InterceptingEventBus(null, interceptors()));
        assertThrows(NullPointerException.class, () -> new InterceptingEventBus(delegate, null));
        assertThrows(NullPointerException.class, () -> new InterceptingEventSink(null, interceptors()));
        assertThrows(NullPointerException.class, () -> new InterceptingEventSink(delegate, null));
        assertThrows(NullPointerException.class, () -> new DelegatingEventBus(null) {
            @Override
            public void describeTo(ComponentDescriptor descriptor) {
            }
        });
    }

    @Test
    void withoutInterceptorsEventsAreDeliveredUnchanged() {
        recordDeliveries();
        EventMessage only = event("only");

        new InterceptingEventBus(delegate, interceptors()).publish(null, List.of(only)).join();

        assertEquals(List.of(only), delivered.get(0));
    }

    @Test
    void interceptorsAreInvokedInOrderForEveryEvent() {
        recordDeliveries();
        List<String> invocations = new ArrayList<>();
        InterceptingEventBus bus = new InterceptingEventBus(delegate, interceptors(
                (message, context, chain) -> {
                    invocations.add("first:" + message.payload());
                    return chain.proceed(message, context);
                },
                (message, context, chain) -> {
                    invocations.add("second:" + message.payload());
                    return chain.proceed(message, context);
                }
        ));

        bus.publish(null, List.of(event("a"), event("b"))).join();

        assertEquals(List.of("first:a", "second:a", "first:b", "second:b"), invocations);
        assertEquals(2, delivered.get(0).size());
    }

    @Test
    void interceptorsCanEnrichEvents() {
        recordDeliveries();
        InterceptingEventBus bus = new InterceptingEventBus(delegate, interceptors(
                (message, context, chain) -> chain.proceed(message.andMetadata(Map.of("key", "value")), context)
        ));

        bus.publish(null, List.of(event("a"))).join();

        assertEquals("value", delivered.get(0).get(0).metadata().get("key"));
    }

    @Test
    void interceptorsCanExpandOneEventIntoMany() {
        recordDeliveries();
        InterceptingEventBus bus = new InterceptingEventBus(delegate, interceptors(
                (message, context, chain) -> MessageStream.fromItems(message, event("extra"))
        ));

        bus.publish(null, List.of(event("a"))).join();

        assertEquals(2, delivered.get(0).size());
    }

    @Test
    void interceptorFailureStopsPublication() {
        recordDeliveries();
        IllegalStateException error = new IllegalStateException("rejected");
        InterceptingEventBus bus = new InterceptingEventBus(delegate, interceptors(
                (message, context, chain) -> MessageStream.failed(error)
        ));

        CompletionException failure =
                assertThrows(CompletionException.class, () -> bus.publish(null, List.of(event("a"))).join());

        assertSame(error, failure.getCause());
        assertTrue(delivered.isEmpty());
    }

    @Test
    void interceptingSinkPublishesToItsDelegate() {
        recordDeliveries();
        InterceptingEventSink sink = new InterceptingEventSink(delegate, interceptors(
                (message, context, chain) -> chain.proceed(message.andMetadata(Map.of("via", "sink")), context)
        ));

        sink.publish(null, event("a")).join();

        assertEquals("sink", delivered.get(0).get(0).metadata().get("via"));
    }

    @Test
    void subscribeIsDelegatedAndCancellable() {
        InterceptingEventBus bus = new InterceptingEventBus(delegate, interceptors());
        List<EventMessage> seen = new ArrayList<>();

        var registration = bus.subscribe((events, context) -> {
            seen.addAll(events);
            return FutureUtils.emptyCompletedFuture();
        });
        delegate.publish(null, List.of(event("a"))).join();
        registration.cancel();
        delegate.publish(null, List.of(event("b"))).join();

        assertEquals(1, seen.size());
    }

    @Test
    void laterChangesToTheGivenInterceptorListDoNotAffectTheBus() {
        recordDeliveries();
        List<MessageDispatchInterceptor<? super EventMessage>> given = interceptors();
        InterceptingEventBus bus = new InterceptingEventBus(delegate, given);
        given.add((message, context, chain) -> MessageStream.failed(new IllegalStateException("late")));

        bus.publish(null, List.of(event("a"))).join();

        assertEquals(1, delivered.size());
    }

    @Test
    void busDescribesItself() {
        Map<String, Object> properties = new HashMap<>();
        List<Object> wrapped = new ArrayList<>();
        List<MessageDispatchInterceptor<? super EventMessage>> given = interceptors();

        new InterceptingEventBus(delegate, given).describeTo(recordingDescriptor(properties, wrapped));

        assertEquals(List.of(delegate), wrapped);
        assertTrue(properties.containsKey("dispatchInterceptors"));
        assertTrue(properties.containsKey("delegateSink"));
    }

    @Test
    void sinkDescribesItself() {
        Map<String, Object> properties = new HashMap<>();
        List<Object> wrapped = new ArrayList<>();

        new InterceptingEventSink(delegate, interceptors()).describeTo(recordingDescriptor(properties, wrapped));

        assertEquals(List.of(delegate), wrapped);
        assertTrue(properties.containsKey("dispatchInterceptors"));
    }

    @Test
    void delegatingEventBusDescribesItselfAsAWrapperOfItsDelegate() {
        Map<String, Object> properties = new HashMap<>();
        List<Object> wrapped = new ArrayList<>();
        DelegatingEventBus bus = new DelegatingEventBus(delegate) {
        };

        bus.describeTo(recordingDescriptor(properties, wrapped));

        assertEquals(List.of(delegate), wrapped);
    }

    @Test
    void delegatingEventBusForwardsPublishAndSubscribe() {
        recordDeliveries();
        DelegatingEventBus bus = new DelegatingEventBus(delegate) {
            @Override
            public void describeTo(ComponentDescriptor descriptor) {
            }
        };
        EventMessage only = event("only");

        CompletableFuture<Void> result = bus.publish(null, List.of(only));
        result.join();

        assertEquals(List.of(only), delivered.get(0));
    }
}
