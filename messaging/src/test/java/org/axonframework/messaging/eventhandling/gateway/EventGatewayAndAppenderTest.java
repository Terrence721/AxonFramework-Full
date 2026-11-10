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

package org.axonframework.messaging.eventhandling.gateway;

import org.axonframework.common.infra.ComponentDescriptor;
import org.axonframework.messaging.core.ClassBasedMessageTypeResolver;
import org.axonframework.messaging.core.EmptyApplicationContext;
import org.axonframework.messaging.core.GenericMessage;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.MessageTypeResolver;
import org.axonframework.messaging.core.Metadata;
import org.axonframework.messaging.core.unitofwork.ProcessingContext;
import org.axonframework.messaging.core.unitofwork.SimpleUnitOfWorkFactory;
import org.axonframework.messaging.eventhandling.EventMessage;
import org.axonframework.messaging.eventhandling.EventSink;
import org.axonframework.messaging.eventhandling.GenericEventMessage;
import org.jspecify.annotations.Nullable;
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
 * Test class validating the {@link DefaultEventGateway}, the {@link EventAppender} and its
 * {@link ProcessingContextEventAppender}, and the shared {@link EventPublishingUtils}.
 */
class EventGatewayAndAppenderTest {

    private final MessageTypeResolver typeResolver = new ClassBasedMessageTypeResolver();
    private final RecordingSink sink = new RecordingSink();
    private final SimpleUnitOfWorkFactory unitOfWorkFactory =
            new SimpleUnitOfWorkFactory(EmptyApplicationContext.INSTANCE);

    private static final class RecordingSink implements EventSink {

        final List<EventMessage> published = new ArrayList<>();
        final List<@Nullable ProcessingContext> contexts = new ArrayList<>();
        CompletableFuture<Void> result = CompletableFuture.completedFuture(null);

        @Override
        public CompletableFuture<Void> publish(@Nullable ProcessingContext context,
                                               List<? extends EventMessage> events) {
            contexts.add(context);
            published.addAll(events);
            return result;
        }

        @Override
        public void describeTo(ComponentDescriptor descriptor) {
        }
    }

    private static ComponentDescriptor recordingDescriptor(Map<String, Object> properties) {
        return (ComponentDescriptor) Proxy.newProxyInstance(
                EventGatewayAndAppenderTest.class.getClassLoader(),
                new Class<?>[]{ComponentDescriptor.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("describeProperty") && args.length == 2) {
                        properties.put((String) args[0], args[1]);
                    }
                    return null;
                }
        );
    }

    private void inContext(java.util.function.Consumer<ProcessingContext> work) {
        unitOfWorkFactory.create().executeWithResult(context -> {
            work.accept(context);
            return CompletableFuture.completedFuture(null);
        }).join();
    }

    // region [EventPublishingUtils]

    @Test
    void eventMessagesAreUsedAsIs() {
        EventMessage event = new GenericEventMessage(new MessageType("e"), "p");

        assertSame(event, EventPublishingUtils.asEventMessage(event, typeResolver));
    }

    @Test
    void eventMessagesGetMetadataAdded() {
        EventMessage event = new GenericEventMessage(new MessageType("e"), "p", Metadata.with("a", "1"));

        EventMessage result = EventPublishingUtils.asEventMessage(event, Metadata.with("b", "2"), typeResolver);

        assertEquals("1", result.metadata().get("a"));
        assertEquals("2", result.metadata().get("b"));
        assertEquals(event.identifier(), result.identifier());
    }

    @Test
    void plainMessagesAreWrappedAsEventsKeepingTheirIdentity() {
        GenericMessage message = new GenericMessage(new MessageType("m"), "p", Metadata.with("a", "1"));

        EventMessage result = EventPublishingUtils.asEventMessage(message, typeResolver);

        assertEquals(message.identifier(), result.identifier());
        assertEquals(message.type(), result.type());
        assertEquals("p", result.payload());
    }

    @Test
    void plainMessagesGetMetadataAddedWhenWrapped() {
        GenericMessage message = new GenericMessage(new MessageType("m"), "p", Metadata.with("a", "1"));

        EventMessage result = EventPublishingUtils.asEventMessage(message, Metadata.with("b", "2"), typeResolver);

        assertEquals("1", result.metadata().get("a"));
        assertEquals("2", result.metadata().get("b"));
    }

    @Test
    void payloadsAreTypedThroughTheResolver() {
        EventMessage result = EventPublishingUtils.asEventMessage("payload", typeResolver);

        assertEquals(typeResolver.resolveOrThrow("payload"), result.type());
        assertEquals("payload", result.payload());
        assertTrue(result.metadata().isEmpty());
    }

    @Test
    void payloadsGetTheGivenMetadata() {
        EventMessage result = EventPublishingUtils.asEventMessage("payload", Metadata.with("k", "v"), typeResolver);

        assertEquals("v", result.metadata().get("k"));
    }

    // endregion

    // region [DefaultEventGateway]

    @Test
    void gatewayPublishesConvertedEventsToTheSinkWithTheContext() {
        DefaultEventGateway gateway = new DefaultEventGateway(sink, typeResolver);

        inContext(context -> gateway.publish(context, "first", 2L).join());

        assertEquals(List.of("first", 2L), sink.published.stream().map(EventMessage::payload).toList());
        assertEquals(1, sink.contexts.size());
    }

    @Test
    void gatewayPublishesWithoutContextByDefault() {
        DefaultEventGateway gateway = new DefaultEventGateway(sink, typeResolver);

        gateway.publish(List.of("first")).join();

        assertEquals(1, sink.published.size());
        assertEquals(null, sink.contexts.get(0));
    }

    @Test
    void gatewayDoesNotTouchTheSinkWhenThereAreNoEvents() {
        DefaultEventGateway gateway = new DefaultEventGateway(sink, typeResolver);

        gateway.publish(null, List.of()).join();

        assertTrue(sink.contexts.isEmpty());
    }

    @Test
    void gatewayReturnsTheSinksFutureSoFailuresPropagate() {
        DefaultEventGateway gateway = new DefaultEventGateway(sink, typeResolver);
        sink.result = CompletableFuture.failedFuture(new IllegalStateException("boom"));

        CompletionException failure =
                assertThrows(CompletionException.class, () -> gateway.publish(List.of("first")).join());

        assertEquals("boom", failure.getCause().getMessage());
    }

    @Test
    void gatewayRejectsNullArguments() {
        DefaultEventGateway gateway = new DefaultEventGateway(sink, typeResolver);

        assertEquals("EventSink may not be null", assertThrows(
                NullPointerException.class, () -> new DefaultEventGateway(null, typeResolver)).getMessage());
        assertEquals("MessageTypeResolver may not be null", assertThrows(
                NullPointerException.class, () -> new DefaultEventGateway(sink, null)).getMessage());
        assertEquals("Events may not be null", assertThrows(
                NullPointerException.class, () -> gateway.publish(null, (List<?>) null)).getMessage());
    }

    @Test
    void gatewayDescribesItsCollaborators() {
        Map<String, Object> properties = new HashMap<>();

        new DefaultEventGateway(sink, typeResolver).describeTo(recordingDescriptor(properties));

        assertSame(sink, properties.get("eventSink"));
        assertSame(typeResolver, properties.get("messageTypeResolver"));
    }

    // endregion

    // region [EventAppender / ProcessingContextEventAppender]

    @Test
    void appenderPublishesEventsToTheSinkWithItsContext() {
        inContext(context -> {
            EventAppender appender = EventAppender.forContext(context, sink, typeResolver);

            appender.append("first", "second");

            assertEquals(List.of("first", "second"), sink.published.stream().map(EventMessage::payload).toList());
            assertSame(context, sink.contexts.get(0));
        });
    }

    @Test
    void appenderAppliesMetadataToEveryEvent() {
        inContext(context -> {
            EventAppender appender = EventAppender.forContext(context, sink, typeResolver);

            appender.append(List.of("first", "second"), Metadata.with("k", "v"));

            assertEquals(2, sink.published.size());
            sink.published.forEach(event -> assertEquals("v", event.metadata().get("k")));
        });
    }

    @Test
    void appenderSingleEventWithMetadataOverloadDelegates() {
        inContext(context -> {
            EventAppender appender = EventAppender.forContext(context, sink, typeResolver);

            appender.append("only", Metadata.with("k", "v"));

            assertEquals(1, sink.published.size());
            assertEquals("v", sink.published.get(0).metadata().get("k"));
        });
    }

    @Test
    void appenderWaitsForThePublishFutureSoFailuresSurfaceImmediately() {
        sink.result = CompletableFuture.failedFuture(new IllegalStateException("boom"));
        inContext(context -> {
            EventAppender appender = EventAppender.forContext(context, sink, typeResolver);

            CompletionException failure =
                    assertThrows(CompletionException.class, () -> appender.append("first"));

            assertEquals("boom", failure.getCause().getMessage());
        });
    }

    @Test
    void appenderRejectsNullArguments() {
        inContext(context -> {
            EventAppender appender = EventAppender.forContext(context, sink, typeResolver);

            assertEquals("Events may not be null", assertThrows(
                    NullPointerException.class, () -> appender.append((List<?>) null)).getMessage());
            assertEquals("Events may not be null", assertThrows(
                    NullPointerException.class, () -> appender.append(null, Metadata.emptyInstance())).getMessage());
            assertEquals("Metadata may not be null", assertThrows(
                    NullPointerException.class, () -> appender.append(List.of("e"), null)).getMessage());
            assertEquals("EventSink may not be null", assertThrows(
                    NullPointerException.class, () -> EventAppender.forContext(context, null, typeResolver)
            ).getMessage());
            assertEquals("MessageTypeResolver may not be null", assertThrows(
                    NullPointerException.class, () -> EventAppender.forContext(context, sink, null)).getMessage());
        });
    }

    @Test
    void forContextRejectsANullContextWithAMessage() {
        assertEquals("ProcessingContext may not be null", assertThrows(
                NullPointerException.class, () -> EventAppender.forContext(null)).getMessage());
        assertEquals("ProcessingContext may not be null", assertThrows(
                NullPointerException.class, () -> EventAppender.forContext(null, sink, typeResolver)).getMessage());
    }

    @Test
    void appenderDescribesItsCollaborators() {
        inContext(context -> {
            Map<String, Object> properties = new HashMap<>();

            EventAppender.forContext(context, sink, typeResolver).describeTo(recordingDescriptor(properties));

            assertSame(context, properties.get("processingContext"));
            assertSame(sink, properties.get("eventSink"));
            assertSame(typeResolver, properties.get("messageTypeResolver"));
        });
    }

    // endregion
}
