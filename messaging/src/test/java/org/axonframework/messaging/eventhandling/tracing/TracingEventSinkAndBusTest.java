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

package org.axonframework.messaging.eventhandling.tracing;

import org.axonframework.common.Registration;
import org.axonframework.common.infra.ComponentDescriptor;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.eventhandling.EventMessage;
import org.axonframework.messaging.eventhandling.GenericEventMessage;
import org.axonframework.messaging.eventhandling.SimpleEventBus;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link TracingEventSink} and the {@link TracingEventBus}.
 */
class TracingEventSinkAndBusTest {

    private final RecordingSpanFactory spanFactory = new RecordingSpanFactory();
    private final SimpleEventBus delegate = new SimpleEventBus();
    private final List<EventMessage> received = new ArrayList<>();

    private static EventMessage event(String name) {
        return new GenericEventMessage(new MessageType(name), "payload");
    }

    private static ComponentDescriptor recordingDescriptor(Map<String, Object> properties, List<Object> wrapped) {
        return (ComponentDescriptor) Proxy.newProxyInstance(
                TracingEventSinkAndBusTest.class.getClassLoader(),
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

    private void subscribeRecorder() {
        delegate.subscribe((events, context) -> {
            received.addAll(events);
            return CompletableFuture.completedFuture(null);
        });
    }

    @Test
    void sinkCreatesADispatchSpanPerEventNamedAfterTheEvent() {
        TracingEventSink sink = new TracingEventSink(delegate, spanFactory);
        subscribeRecorder();

        sink.publish(null, List.of(event("first"), event("second"))).join();

        assertEquals(List.of("dispatch:EventSink.publish first", "dispatch:EventSink.publish second"),
                     spanFactory.created);
        assertEquals(2, received.size());
    }

    @Test
    void sinkPropagatesTracingContextOntoEachEventWithinAShortLivedSpan() {
        TracingEventSink sink = new TracingEventSink(delegate, spanFactory);
        subscribeRecorder();

        sink.publish(null, List.of(event("first"))).join();

        assertEquals(List.of("start", "propagate", "close"), spanFactory.spans.get(0).events);
    }

    @Test
    void sinkPublishesEventsInOrder() {
        TracingEventSink sink = new TracingEventSink(delegate, spanFactory);
        subscribeRecorder();
        EventMessage first = event("first");
        EventMessage second = event("second");

        sink.publish(null, List.of(first, second)).join();

        assertEquals(List.of(first.identifier(), second.identifier()),
                     List.of(received.get(0).identifier(), received.get(1).identifier()));
    }

    @Test
    void sinkWithNoEventsCreatesNoSpans() {
        TracingEventSink sink = new TracingEventSink(delegate, spanFactory);

        sink.publish(null, List.of()).join();

        assertTrue(spanFactory.created.isEmpty());
    }

    @Test
    void sinkPropagatesSubscriberFailures() {
        TracingEventSink sink = new TracingEventSink(delegate, spanFactory);
        delegate.subscribe((events, context) -> CompletableFuture.failedFuture(new IllegalStateException("boom")));

        CompletableFuture<Void> result = sink.publish(null, List.of(event("first")));

        assertTrue(result.isCompletedExceptionally());
    }

    @Test
    void sinkRejectsNullArguments() {
        assertEquals("delegate may not be null", assertThrows(
                NullPointerException.class, () -> new TracingEventSink(null, spanFactory)).getMessage());
        assertEquals("spanFactory may not be null", assertThrows(
                NullPointerException.class, () -> new TracingEventSink(delegate, null)).getMessage());
    }

    @Test
    void sinkDescribesDelegateAndSpanFactory() {
        Map<String, Object> properties = new HashMap<>();
        List<Object> wrapped = new ArrayList<>();

        new TracingEventSink(delegate, spanFactory).describeTo(recordingDescriptor(properties, wrapped));

        assertEquals(List.of(delegate), wrapped);
        assertSame(spanFactory, properties.get("spanFactory"));
    }

    @Test
    void busPublishesThroughTheTracingSink() {
        TracingEventBus bus = new TracingEventBus(delegate, spanFactory);
        subscribeRecorder();

        bus.publish(null, List.of(event("first"))).join();

        assertEquals(List.of("dispatch:EventSink.publish first"), spanFactory.created);
        assertEquals(1, received.size());
    }

    @Test
    void busSubscribesStraightOnTheDelegateWithoutTracing() {
        TracingEventBus bus = new TracingEventBus(delegate, spanFactory);
        List<EventMessage> viaBus = new ArrayList<>();

        Registration registration = bus.subscribe((events, context) -> {
            viaBus.addAll(events);
            return CompletableFuture.completedFuture(null);
        });
        delegate.publish(null, List.of(event("direct"))).join();

        assertEquals(1, viaBus.size());
        assertTrue(spanFactory.created.isEmpty());
        assertTrue(registration.cancel());
        delegate.publish(null, List.of(event("after"))).join();
        assertEquals(1, viaBus.size());
    }

    @Test
    void busRejectsNullDelegate() {
        assertEquals("delegate may not be null", assertThrows(
                NullPointerException.class, () -> new TracingEventBus(null, spanFactory)).getMessage());
    }

    @Test
    void busDescribesDelegateAndItsSink() {
        Map<String, Object> properties = new HashMap<>();
        List<Object> wrapped = new ArrayList<>();

        new TracingEventBus(delegate, spanFactory).describeTo(recordingDescriptor(properties, wrapped));

        assertEquals(List.of(delegate), wrapped);
        assertInstanceOf(TracingEventSink.class, properties.get("delegateSink"));
    }
}
