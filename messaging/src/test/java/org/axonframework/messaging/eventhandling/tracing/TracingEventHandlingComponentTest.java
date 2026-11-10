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

import org.axonframework.common.infra.ComponentDescriptor;
import org.axonframework.messaging.core.EmptyApplicationContext;
import org.axonframework.messaging.core.Metadata;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.unitofwork.SimpleUnitOfWorkFactory;
import org.axonframework.messaging.eventhandling.EventHandler;
import org.axonframework.messaging.eventhandling.EventMessage;
import org.axonframework.messaging.eventhandling.GenericEventMessage;
import org.axonframework.messaging.eventhandling.SimpleEventHandlingComponent;
import org.axonframework.messaging.eventhandling.replay.GenericReplayStatusChanged;
import org.axonframework.messaging.eventhandling.replay.GenericResetContext;
import org.axonframework.messaging.eventhandling.replay.ReplayStatus;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link TracingEventHandlingComponent} span topology for each combination of the streaming,
 * batch and distributed-in-same-trace settings.
 */
class TracingEventHandlingComponentTest {

    private static final MessageType TYPE = new MessageType("test.event");
    private static final String PROCESS_SPAN = "EventProcessor.process test.event";
    private static final String BATCH_SPAN = "StreamingEventProcessor.batch";
    private static final String PROCESSOR_ATTRIBUTE = "attribute:axoniq.event_processor.name=orders";

    private final RecordingSpanFactory spanFactory = new RecordingSpanFactory();
    private final SimpleUnitOfWorkFactory unitOfWorkFactory =
            new SimpleUnitOfWorkFactory(EmptyApplicationContext.INSTANCE);
    private final List<String> handled = new ArrayList<>();

    private SimpleEventHandlingComponent delegate() {
        SimpleEventHandlingComponent delegate = SimpleEventHandlingComponent.create("delegate");
        delegate.subscribe(TYPE.qualifiedName(), (event, context) -> {
            handled.add(event.identifier());
            return MessageStream.empty();
        });
        return delegate;
    }

    private TracingEventHandlingComponent component(boolean streaming,
                                                    boolean batchTraceEnabled,
                                                    boolean distributedInSameTrace) {
        return new TracingEventHandlingComponent(delegate(), spanFactory, "orders", streaming, batchTraceEnabled,
                                                 distributedInSameTrace, Duration.ofMinutes(2));
    }

    private static EventMessage freshEvent() {
        return new GenericEventMessage(TYPE, "payload");
    }

    private static EventMessage staleEvent() {
        return new GenericEventMessage("stale", TYPE, "payload", Metadata.emptyInstance(),
                                       Instant.now().minus(Duration.ofHours(1)));
    }

    private void handleAll(TracingEventHandlingComponent component, EventMessage... events) {
        unitOfWorkFactory.create().executeWithResult(context -> {
            for (EventMessage event : events) {
                component.handle(event, context).first().asCompletableFuture().join();
            }
            return CompletableFuture.completedFuture(null);
        }).join();
    }

    @Test
    void nonStreamingContinuesThePublishersTraceWithoutABatchSpan() {
        handleAll(component(false, true, false), freshEvent(), freshEvent());

        assertEquals(List.of("handler:" + PROCESS_SPAN, "handler:" + PROCESS_SPAN), spanFactory.created);
        assertEquals(2, handled.size());
    }

    @Test
    void streamingWithBatchTracingParentsHandlersUnderASingleBatchSpan() {
        handleAll(component(true, true, false), freshEvent(), freshEvent());

        assertEquals(List.of("root:" + BATCH_SPAN,
                             "contextParentHandler:" + PROCESS_SPAN,
                             "contextParentHandler:" + PROCESS_SPAN),
                     spanFactory.created);
    }

    @Test
    void batchSpanIsSharedAcrossComponentInstancesOnTheSameContext() {
        TracingEventHandlingComponent first = component(true, true, false);
        TracingEventHandlingComponent second = component(true, true, false);

        unitOfWorkFactory.create().executeWithResult(context -> {
            first.handle(freshEvent(), context).first().asCompletableFuture().join();
            second.handle(freshEvent(), context).first().asCompletableFuture().join();
            return CompletableFuture.completedFuture(null);
        }).join();

        assertEquals(1, spanFactory.created.stream().filter(name -> name.startsWith("root:")).count());
    }

    @Test
    void batchSpanLifecycleCoversTheWholeProcessingContext() {
        handleAll(component(true, true, false), freshEvent());

        RecordingSpanFactory.RecordingSpan batch = spanFactory.spans.get(0);
        assertEquals(List.of(PROCESSOR_ATTRIBUTE, "start", "close"), batch.events);
    }

    @Test
    void streamingWithoutBatchTracingStartsLinkedTracesPerEvent() {
        handleAll(component(true, false, false), freshEvent());

        assertEquals(List.of("disconnectedHandler:" + PROCESS_SPAN), spanFactory.created);
    }

    @Test
    void streamingInSameTraceContinuesThePublishersTraceForFreshEvents() {
        handleAll(component(true, true, true), freshEvent());

        assertEquals(List.of("handler:" + PROCESS_SPAN), spanFactory.created);
    }

    @Test
    void streamingInSameTraceStartsALinkedTraceForStaleEventsWithoutABatchSpan() {
        handleAll(component(true, true, true), staleEvent());

        assertEquals(List.of("disconnectedHandler:" + PROCESS_SPAN), spanFactory.created);
    }

    @Test
    void nonStreamingIgnoresTheStalenessOfTheEvent() {
        handleAll(component(false, true, true), staleEvent());

        assertEquals(List.of("handler:" + PROCESS_SPAN), spanFactory.created);
    }

    @Test
    void handlerSpanCarriesTheProcessorNameAndClosesWhenTheEventIsHandled() {
        handleAll(component(false, true, false), freshEvent());

        RecordingSpanFactory.RecordingSpan span = spanFactory.spans.get(0);
        assertEquals(List.of(PROCESSOR_ATTRIBUTE, "start", "close"), span.events);
    }

    @Test
    void spansHaveNoProcessorAttributeWhenTheNameIsUnknown() {
        TracingEventHandlingComponent component = new TracingEventHandlingComponent(delegate(), spanFactory);

        handleAll(component, freshEvent());

        assertEquals(List.of("start", "close"), spanFactory.spans.get(0).events);
    }

    @Test
    void delegateFailureIsRecordedOnTheSpanAndPropagated() {
        SimpleEventHandlingComponent failing = SimpleEventHandlingComponent.create("failing");
        failing.subscribe(TYPE.qualifiedName(),
                          (EventHandler) (event, context) -> MessageStream.failed(new IllegalStateException("boom")));
        TracingEventHandlingComponent component =
                new TracingEventHandlingComponent(failing, spanFactory, "orders", false, true, false,
                                                  Duration.ofMinutes(2));

        CompletionException failure = assertThrows(CompletionException.class, () -> handleAll(component, freshEvent()));

        assertEquals("boom", failure.getCause().getMessage());
        RecordingSpanFactory.RecordingSpan span = spanFactory.spans.get(0);
        assertEquals(1, span.exceptions.size());
        assertTrue(span.closed);
    }

    @Test
    void delegatesEverythingElseWithoutTracing() {
        SimpleEventHandlingComponent delegate = delegate();
        TracingEventHandlingComponent component = new TracingEventHandlingComponent(delegate, spanFactory);

        assertEquals(delegate.supportedEvents(), component.supportedEvents());
        assertEquals(delegate.supportsReset(), component.supportsReset());
        unitOfWorkFactory.create().executeWithResult(context -> {
            EventMessage event = freshEvent();
            assertEquals(delegate.sequenceIdentifierFor(event, context),
                         component.sequenceIdentifierFor(event, context));
            component.handle(new GenericResetContext(new MessageType("reset"), null), context)
                     .first().asCompletableFuture().join();
            component.handle(new GenericReplayStatusChanged(ReplayStatus.REPLAY, null), context)
                     .first().asCompletableFuture().join();
            return CompletableFuture.completedFuture(null);
        }).join();

        assertTrue(spanFactory.created.isEmpty());
    }

    @Test
    void rejectsNullArguments() {
        SimpleEventHandlingComponent delegate = delegate();
        assertEquals("delegate may not be null", assertThrows(
                NullPointerException.class, () -> new TracingEventHandlingComponent(null, spanFactory)).getMessage());
        assertEquals("spanFactory may not be null", assertThrows(
                NullPointerException.class, () -> new TracingEventHandlingComponent(delegate, null)).getMessage());
        assertEquals("distributedInSameTraceTimeLimit may not be null", assertThrows(
                NullPointerException.class,
                () -> new TracingEventHandlingComponent(delegate, spanFactory, null, false, true, false, null)
        ).getMessage());
    }

    @Test
    void describesDelegateAndSpanFactory() {
        SimpleEventHandlingComponent delegate = delegate();
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

        new TracingEventHandlingComponent(delegate, spanFactory).describeTo(descriptor);

        assertEquals(List.of(delegate), wrapped);
        assertSame(spanFactory, properties.get("spanFactory"));
        assertFalse(properties.isEmpty());
    }
}
