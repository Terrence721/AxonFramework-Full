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

import org.axonframework.common.infra.ComponentDescriptor;
import org.axonframework.messaging.core.EmptyApplicationContext;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.QualifiedName;
import org.axonframework.messaging.core.sequencing.SequencingPolicy;
import org.axonframework.messaging.core.unitofwork.SimpleUnitOfWorkFactory;
import org.axonframework.messaging.eventhandling.replay.GenericReplayStatusChanged;
import org.axonframework.messaging.eventhandling.replay.GenericResetContext;
import org.axonframework.messaging.eventhandling.replay.ReplayStatus;
import org.axonframework.messaging.eventhandling.replay.ReplayStatusChanged;
import org.axonframework.messaging.eventhandling.replay.ResetContext;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link SimpleEventHandlingComponent} and the {@link DelegatingEventHandlingComponent}.
 */
class SimpleEventHandlingComponentTest {

    private static final MessageType TYPE = new MessageType("test.event");
    private static final QualifiedName NAME = TYPE.qualifiedName();
    private static final QualifiedName OTHER_NAME = new MessageType("other.event").qualifiedName();

    private final SimpleEventHandlingComponent component = SimpleEventHandlingComponent.create("test");
    private final SimpleUnitOfWorkFactory unitOfWorkFactory =
            new SimpleUnitOfWorkFactory(EmptyApplicationContext.INSTANCE);
    private final List<String> calls = new ArrayList<>();

    private static EventMessage event() {
        return new GenericEventMessage(TYPE, "payload");
    }

    private static ResetContext resetContext() {
        return new GenericResetContext(new MessageType("reset"), null);
    }

    private static ReplayStatusChanged statusChange() {
        return new GenericReplayStatusChanged(ReplayStatus.REPLAY, null);
    }

    private EventHandler recording(String name) {
        return (event, context) -> {
            calls.add(name);
            return MessageStream.empty();
        };
    }

    private static void consume(MessageStream<?> stream) {
        stream.first().asCompletableFuture().join();
    }

    private static ComponentDescriptor recordingDescriptor(Map<String, Object> properties, List<Object> wrapped) {
        return (ComponentDescriptor) Proxy.newProxyInstance(
                SimpleEventHandlingComponentTest.class.getClassLoader(),
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

    // --- creation ---

    @Test
    void createRejectsInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> SimpleEventHandlingComponent.create(null));
        assertThrows(IllegalArgumentException.class, () -> SimpleEventHandlingComponent.create(""));
        assertThrows(NullPointerException.class, () -> SimpleEventHandlingComponent.create("name", null));
    }

    // --- event handling ---

    @Test
    void handleWithoutHandlersFailsWithNoHandlerForEvent() {
        CompletionException failure =
                assertThrows(CompletionException.class, () -> consume(component.handle(event(), null)));

        NoHandlerForEventException cause = assertInstanceOf(NoHandlerForEventException.class, failure.getCause());
        assertTrue(cause.getMessage().contains(NAME.toString()));
    }

    @Test
    void handleInvokesTheSubscribedHandler() {
        component.subscribe(NAME, recording("handler"));

        consume(component.handle(event(), null));

        assertEquals(List.of("handler"), calls);
    }

    @Test
    void handleInvokesAllHandlersOfAnEventNameInSubscriptionOrder() {
        component.subscribe(NAME, recording("first")).subscribe(NAME, recording("second"));

        consume(component.handle(event(), null));

        assertEquals(List.of("first", "second"), calls);
    }

    @Test
    void handleOnlyInvokesHandlersOfTheEventName() {
        component.subscribe(NAME, recording("match")).subscribe(OTHER_NAME, recording("other"));

        consume(component.handle(event(), null));

        assertEquals(List.of("match"), calls);
    }

    @Test
    void subscribingForMultipleNamesRegistersTheHandlerForEach() {
        component.subscribe(Set.of(NAME, OTHER_NAME), recording("shared"));

        assertEquals(Set.of(NAME, OTHER_NAME), component.supportedEvents());
        assertTrue(component.supports(NAME));
        assertTrue(component.supports(OTHER_NAME));
    }

    @Test
    void handlerFailureIsReportedThroughTheStream() {
        IllegalStateException error = new IllegalStateException("boom");
        component.subscribe(NAME, (event, context) -> MessageStream.failed(error));

        CompletionException failure =
                assertThrows(CompletionException.class, () -> consume(component.handle(event(), null)));

        assertSame(error, failure.getCause());
    }

    @Test
    void subscribeRejectsANullHandler() {
        assertThrows(NullPointerException.class, () -> component.subscribe(NAME, (EventHandler) null));
    }

    @Test
    void resetAndReplayHandlerSubscriptionsRejectNullWithADescriptiveMessage() {
        NullPointerException reset = assertThrows(
                NullPointerException.class,
                () -> component.subscribe((org.axonframework.messaging.eventhandling.replay.ResetHandler) null)
        );
        NullPointerException replay = assertThrows(
                NullPointerException.class,
                () -> component.subscribe(
                        (org.axonframework.messaging.eventhandling.replay.ReplayStatusChangedHandler) null
                )
        );

        assertEquals("The reset handler cannot be null.", reset.getMessage());
        assertEquals("The replay status changed handler cannot be null.", replay.getMessage());
    }

    @Test
    void supportedEventsReflectsSubscriptionsAndIsASnapshot() {
        Set<QualifiedName> before = component.supportedEvents();
        component.subscribe(NAME, recording("handler"));

        assertTrue(before.isEmpty());
        assertEquals(Set.of(NAME), component.supportedEvents());
        assertFalse(component.supports(OTHER_NAME));
    }

    // --- sequencing ---

    @Test
    void defaultSequencingPolicyYieldsAStableIdentifierForEventsWithoutAnAggregate() {
        List<Object> identifiers = new ArrayList<>();

        unitOfWorkFactory.create().executeWithResult(ctx -> {
            identifiers.add(component.sequenceIdentifierFor(event(), ctx));
            identifiers.add(component.sequenceIdentifierFor(event(), ctx));
            return CompletableFuture.completedFuture(null);
        }).join();

        assertEquals(identifiers.get(0), identifiers.get(1));
    }

    @Test
    void sequencingUsesTheConfiguredPolicy() {
        SequencingPolicy<EventMessage> policy = (message, context) -> Optional.of("custom");

        assertEquals("custom", SimpleEventHandlingComponent.create("test", policy)
                                                           .sequenceIdentifierFor(event(), null));
    }

    @Test
    void sequencingFallsBackToTheEventIdentifierWhenThePolicyResolvesNothing() {
        SequencingPolicy<EventMessage> policy = (message, context) -> Optional.empty();
        EventMessage event = event();

        assertEquals(event.identifier(), SimpleEventHandlingComponent.create("test", policy)
                                                                     .sequenceIdentifierFor(event, null));
    }

    @Test
    void sequencingDelegatesToANestedEventHandlingComponent() {
        SequencingPolicy<EventMessage> nestedPolicy = (message, context) -> Optional.of("nested");
        SimpleEventHandlingComponent nested = SimpleEventHandlingComponent.create("nested", nestedPolicy);
        nested.subscribe(NAME, recording("nested"));
        component.subscribe(NAME, nested);

        assertEquals("nested", component.sequenceIdentifierFor(event(), null));
    }

    @Test
    void sequencingUsesThePolicyWhenNoHandlerIsAnEventHandlingComponent() {
        SequencingPolicy<EventMessage> policy = (message, context) -> Optional.of("policy");
        SimpleEventHandlingComponent subject = SimpleEventHandlingComponent.create("test", policy);
        subject.subscribe(NAME, recording("plain"));

        assertEquals("policy", subject.sequenceIdentifierFor(event(), null));
    }

    // --- reset and replay handling ---

    @Test
    void resetWithoutHandlersCompletesEmpty() {
        consume(component.handle(resetContext(), null));
    }

    @Test
    void resetInvokesEverySubscribedResetHandler() {
        component.subscribe((org.axonframework.messaging.eventhandling.replay.ResetHandler) (reset, context) -> {
            calls.add("reset-1");
            return MessageStream.empty();
        });
        component.subscribe((org.axonframework.messaging.eventhandling.replay.ResetHandler) (reset, context) -> {
            calls.add("reset-2");
            return MessageStream.empty();
        });

        consume(component.handle(resetContext(), null));

        assertEquals(2, calls.size());
        assertTrue(calls.containsAll(List.of("reset-1", "reset-2")));
    }

    @Test
    void replayStatusChangeWithoutHandlersCompletesEmpty() {
        consume(component.handle(statusChange(), null));
    }

    @Test
    void replayStatusChangeInvokesEverySubscribedHandler() {
        component.subscribe((org.axonframework.messaging.eventhandling.replay.ReplayStatusChangedHandler)
                                    (change, context) -> {
                                        calls.add("status:" + change.status());
                                        return MessageStream.empty();
                                    });

        consume(component.handle(statusChange(), null));

        assertEquals(List.of("status:REPLAY"), calls);
    }

    @Test
    void resetFailureIsReportedThroughTheStream() {
        IllegalStateException error = new IllegalStateException("reset failed");
        component.subscribe((org.axonframework.messaging.eventhandling.replay.ResetHandler)
                                    (reset, context) -> MessageStream.failed(error));

        CompletionException failure =
                assertThrows(CompletionException.class, () -> consume(component.handle(resetContext(), null)));

        assertSame(error, failure.getCause());
    }

    @Test
    void supportsResetByDefault() {
        assertTrue(component.supportsReset());
    }

    // --- description ---

    @Test
    void describesItself() {
        Map<String, Object> properties = new HashMap<>();
        component.subscribe(NAME, recording("handler"));

        component.describeTo(recordingDescriptor(properties, new ArrayList<>()));

        assertEquals("test", properties.get("name"));
        assertTrue(properties.containsKey("eventHandlers"));
        assertTrue(properties.containsKey("resetHandlers"));
        assertTrue(properties.containsKey("replayStatusChangedHandlers"));
        assertTrue(properties.containsKey("sequencingPolicy"));
    }

    // --- DelegatingEventHandlingComponent ---

    private DelegatingEventHandlingComponent delegating() {
        return new DelegatingEventHandlingComponent(component) {
        };
    }

    @Test
    void delegatingComponentRejectsANullDelegate() {
        assertThrows(NullPointerException.class, () -> new DelegatingEventHandlingComponent(null) {
        });
    }

    @Test
    void delegatingComponentForwardsEverything() {
        component.subscribe(NAME, recording("handler"));
        component.subscribe((org.axonframework.messaging.eventhandling.replay.ResetHandler) (reset, context) -> {
            calls.add("reset");
            return MessageStream.empty();
        });
        DelegatingEventHandlingComponent delegating = delegating();

        consume(delegating.handle(event(), null));
        consume(delegating.handle(resetContext(), null));

        assertEquals(List.of("handler", "reset"), calls);
        assertEquals(component.supportedEvents(), delegating.supportedEvents());
        assertTrue(delegating.supports(NAME));
        assertEquals(component.supportsReset(), delegating.supportsReset());
        assertEquals("custom", SimpleEventHandlingComponent.create("custom", (m, c) -> Optional.of("custom"))
                                                           .sequenceIdentifierFor(event(), null));
        DelegatingEventHandlingComponent customDelegating = new DelegatingEventHandlingComponent(
                SimpleEventHandlingComponent.create("custom", (m, c) -> Optional.of("custom"))
        ) {
        };
        assertEquals("custom", customDelegating.sequenceIdentifierFor(event(), null));
    }

    @Test
    void delegatingComponentUnwrapsItselfOrItsDelegate() {
        DelegatingEventHandlingComponent delegating = delegating();

        assertSame(delegating, delegating.unwrap(DelegatingEventHandlingComponent.class).orElseThrow());
        assertSame(component, delegating.unwrap(SimpleEventHandlingComponent.class).orElseThrow());
        assertTrue(delegating.unwrap(String.class).isEmpty());
    }

    @Test
    void delegatingComponentDescribesItselfAsAWrapper() {
        List<Object> wrapped = new ArrayList<>();

        delegating().describeTo(recordingDescriptor(new HashMap<>(), wrapped));

        assertEquals(List.of(component), wrapped);
    }
}
