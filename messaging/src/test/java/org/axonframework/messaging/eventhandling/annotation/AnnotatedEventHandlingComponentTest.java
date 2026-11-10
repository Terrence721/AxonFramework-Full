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

package org.axonframework.messaging.eventhandling.annotation;

import org.axonframework.common.infra.ComponentDescriptor;
import org.axonframework.conversion.Converter;
import org.axonframework.messaging.core.ClassBasedMessageTypeResolver;
import org.axonframework.messaging.core.EmptyApplicationContext;
import org.axonframework.messaging.core.Metadata;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.MessageTypeResolver;
import org.axonframework.messaging.core.QualifiedName;
import org.axonframework.messaging.core.annotation.AnnotatedMessageHandlingMemberDefinition;
import org.axonframework.messaging.core.annotation.DefaultParameterResolverFactory;
import org.axonframework.messaging.core.annotation.MultiHandlerDefinition;
import org.axonframework.messaging.core.annotation.MultiHandlerEnhancerDefinition;
import org.axonframework.messaging.core.annotation.UnsupportedHandlerException;
import org.axonframework.messaging.core.sequencing.MetadataSequencingPolicy;
import org.axonframework.messaging.core.annotation.SequencingPolicy;
import org.axonframework.messaging.core.unitofwork.SimpleUnitOfWorkFactory;
import org.axonframework.messaging.core.unitofwork.UnitOfWorkFactory;
import org.axonframework.messaging.eventhandling.EventMessage;
import org.axonframework.messaging.eventhandling.GenericEventMessage;
import org.axonframework.messaging.eventhandling.conversion.DelegatingEventConverter;
import org.axonframework.messaging.eventhandling.conversion.EventConverter;
import org.axonframework.messaging.eventhandling.replay.GenericReplayStatusChanged;
import org.axonframework.messaging.eventhandling.replay.GenericResetContext;
import org.axonframework.messaging.eventhandling.replay.ReplayStatus;
import org.axonframework.messaging.eventhandling.replay.annotation.AllowReplay;
import org.axonframework.messaging.eventhandling.replay.annotation.DisallowReplay;
import org.axonframework.messaging.eventhandling.replay.annotation.ReplayStatusChangedHandler;
import org.axonframework.messaging.eventhandling.replay.annotation.ResetHandler;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link AnnotatedEventHandlingComponent}, the {@link MethodEventHandlerDefinition} and the
 * {@link MethodSequencingPolicyEventHandlerDefinition}.
 */
class AnnotatedEventHandlingComponentTest {

    private static final DefaultParameterResolverFactory PARAMETERS = new DefaultParameterResolverFactory();
    private static final MultiHandlerDefinition HANDLERS = MultiHandlerDefinition.ordered(
            MultiHandlerEnhancerDefinition.ordered(
                    new MethodEventHandlerDefinition(),
                    new MethodSequencingPolicyEventHandlerDefinition()
            ),
            new AnnotatedMessageHandlingMemberDefinition()
    );

    private static final Converter IDENTITY_CONVERTER = new Converter() {
        @SuppressWarnings("unchecked")
        @Override
        public <T> @Nullable T convert(@Nullable Object input, Type targetType) {
            return (T) input;
        }

        @Override
        public void describeTo(ComponentDescriptor descriptor) {
        }
    };

    private final MessageTypeResolver typeResolver = new ClassBasedMessageTypeResolver();
    private final EventConverter converter = new DelegatingEventConverter(IDENTITY_CONVERTER);
    private final UnitOfWorkFactory unitOfWorkFactory = new SimpleUnitOfWorkFactory(EmptyApplicationContext.INSTANCE);

    static class Handlers {

        final List<String> calls = new CopyOnWriteArrayList<>();

        @EventHandler(eventName = "greet")
        void greet(String name) {
            calls.add("greet:" + name);
        }

        @EventHandler(eventName = "boom")
        void boom(String ignored) {
            throw new IllegalStateException("kaboom");
        }

        @EventHandler
        void lengthOf(Long payload) {
            calls.add("long:" + payload);
        }

        @ResetHandler
        void onReset() {
            calls.add("reset");
        }

        @ReplayStatusChangedHandler
        void onReplayStatus(org.axonframework.messaging.eventhandling.replay.ReplayStatusChanged change) {
            calls.add("status:" + change.status());
        }
    }

    static class DisallowingHandlers {

        @AllowReplay(false)
        @EventHandler(eventName = "only")
        void only(String ignored) {
        }
    }

    static class MixedReplayHandlers {

        @DisallowReplay
        @EventHandler(eventName = "disallowed")
        void disallowed(String ignored) {
        }

        @EventHandler(eventName = "allowed")
        void allowed(String ignored) {
        }
    }

    static class SequencedHandlers {

        @SequencingPolicy(type = MetadataSequencingPolicy.class, parameters = "tenant")
        @EventHandler(eventName = "sequenced")
        void sequenced(String ignored) {
        }

        @EventHandler(eventName = "plain")
        void plain(String ignored) {
        }
    }

    @SequencingPolicy(type = MetadataSequencingPolicy.class, parameters = "tenant")
    static class ClassSequencedHandlers {

        @EventHandler(eventName = "classLevel")
        void classLevel(String ignored) {
        }
    }

    static class NoNoArgPolicy implements org.axonframework.messaging.core.sequencing.SequencingPolicy<EventMessage> {

        NoNoArgPolicy(String ignored) {
        }

        @Override
        public java.util.Optional<Object> sequenceIdentifierFor(EventMessage message,
                                                                 org.axonframework.messaging.core.unitofwork.ProcessingContext context) {
            return java.util.Optional.empty();
        }
    }

    static class MissingNoArgConstructorHandlers {

        @SequencingPolicy(type = NoNoArgPolicy.class)
        @EventHandler(eventName = "bad")
        void bad(String ignored) {
        }
    }

    static class NoMatchingConstructorHandlers {

        @SequencingPolicy(type = MetadataSequencingPolicy.class, parameters = {"a", "b"})
        @EventHandler(eventName = "bad")
        void bad(String ignored) {
        }
    }

    private <T> AnnotatedEventHandlingComponent<T> component(T handlers) {
        return new AnnotatedEventHandlingComponent<>(handlers, PARAMETERS, HANDLERS, typeResolver, converter);
    }

    private <R> R inContext(java.util.function.Function<org.axonframework.messaging.core.unitofwork.ProcessingContext, R> work) {
        return unitOfWorkFactory.create()
                                .executeWithResult(context -> CompletableFuture.completedFuture(work.apply(context)))
                                .join();
    }

    private static EventMessage event(String name, Object payload) {
        return new GenericEventMessage(new MessageType(name), payload);
    }

    @Test
    void supportsEventsByExplicitNameAndByResolvedPayloadType() {
        Set<QualifiedName> supported = component(new Handlers()).supportedEvents();

        assertEquals(
                Set.of(new QualifiedName("greet"),
                       new QualifiedName("boom"),
                       typeResolver.resolveOrThrow(Long.class).qualifiedName()),
                supported
        );
    }

    @Test
    void supportedEventsAreACopy() {
        AnnotatedEventHandlingComponent<Handlers> component = component(new Handlers());

        assertThrows(UnsupportedOperationException.class, () -> component.supportedEvents().clear());
    }

    @Test
    void invokesTheHandlerMatchingTheEventName() {
        Handlers handlers = new Handlers();
        AnnotatedEventHandlingComponent<Handlers> component = component(handlers);

        inContext(ctx -> component.handle(event("greet", "Axon"), ctx).first().asCompletableFuture().join());

        assertEquals(List.of("greet:Axon"), handlers.calls);
    }

    @Test
    void invokesTheHandlerResolvedFromThePayloadType() {
        Handlers handlers = new Handlers();
        AnnotatedEventHandlingComponent<Handlers> component = component(handlers);
        MessageType type = typeResolver.resolveOrThrow(Long.class);

        inContext(ctx -> component.handle(new GenericEventMessage(type, 5L), ctx)
                                  .first().asCompletableFuture().join());

        assertEquals(List.of("long:5"), handlers.calls);
    }

    @Test
    void handlerFailureSurfacesThroughTheStream() {
        AnnotatedEventHandlingComponent<Handlers> component = component(new Handlers());

        CompletionException failure = assertThrows(CompletionException.class, () -> inContext(
                ctx -> component.handle(event("boom", "x"), ctx).first().asCompletableFuture().join()
        ));

        assertTrue(rootCause(failure).getMessage().contains("kaboom"));
    }

    @Test
    void invokesResetHandlers() {
        Handlers handlers = new Handlers();
        AnnotatedEventHandlingComponent<Handlers> component = component(handlers);

        inContext(ctx -> component.handle(new GenericResetContext(new MessageType("reset"), null), ctx)
                                  .first().asCompletableFuture().join());

        assertEquals(List.of("reset"), handlers.calls);
    }

    @Test
    void invokesReplayStatusChangedHandlers() {
        Handlers handlers = new Handlers();
        AnnotatedEventHandlingComponent<Handlers> component = component(handlers);

        inContext(ctx -> component.handle(new GenericReplayStatusChanged(ReplayStatus.REPLAY, null), ctx)
                                  .first().asCompletableFuture().join());

        assertEquals(List.of("status:REPLAY"), handlers.calls);
    }

    @Test
    void supportsResetByDefault() {
        assertTrue(component(new Handlers()).supportsReset());
    }

    @Test
    void doesNotSupportResetWhenAllHandlersDisallowReplay() {
        assertFalse(component(new DisallowingHandlers()).supportsReset());
    }

    @Test
    void supportsResetWhenAtLeastOneHandlerAllowsReplay() {
        assertTrue(component(new MixedReplayHandlers()).supportsReset());
    }

    @Test
    void customSequencingPolicyDeterminesSequenceIdentifier() {
        AnnotatedEventHandlingComponent<SequencedHandlers> component = component(new SequencedHandlers());
        EventMessage event = new GenericEventMessage(new MessageType("sequenced"), "x", Metadata.with("tenant", "t1"));

        Object identifier = inContext(ctx -> component.sequenceIdentifierFor(event, ctx));

        assertEquals("t1", identifier);
    }

    @Test
    void handlersWithoutCustomPolicyFallBackToTheComponentPolicy() {
        AnnotatedEventHandlingComponent<SequencedHandlers> component = component(new SequencedHandlers());
        EventMessage event = new GenericEventMessage(new MessageType("plain"), "x", Metadata.with("tenant", "t1"));

        Object identifier = inContext(ctx -> component.sequenceIdentifierFor(event, ctx));

        assertFalse("t1".equals(identifier));
    }

    @Test
    void classLevelSequencingPolicyAppliesToHandlerMethods() {
        AnnotatedEventHandlingComponent<ClassSequencedHandlers> component = component(new ClassSequencedHandlers());
        EventMessage event = new GenericEventMessage(new MessageType("classLevel"), "x", Metadata.with("tenant", "t2"));

        Object identifier = inContext(ctx -> component.sequenceIdentifierFor(event, ctx));

        assertEquals("t2", identifier);
    }

    @Test
    void sequencingPolicyWithoutNoArgConstructorIsRejected() {
        assertThrows(UnsupportedHandlerException.class, () -> component(new MissingNoArgConstructorHandlers()));
    }

    @Test
    void sequencingPolicyWithoutMatchingConstructorIsRejected() {
        assertThrows(UnsupportedHandlerException.class, () -> component(new NoMatchingConstructorHandlers()));
    }

    @Test
    void unwrapReturnsTheComponentItself() {
        AnnotatedEventHandlingComponent<Handlers> component = component(new Handlers());

        assertSame(component, component.unwrap(AnnotatedEventHandlingComponent.class).orElseThrow());
    }

    @Test
    void unwrapResolvesBehaviorOfTheTarget() {
        assertTrue(component(new Handlers()).unwrap(String.class).isEmpty());
    }

    @Test
    void rejectsNullArguments() {
        Handlers handlers = new Handlers();

        assertEquals("The Annotated Event Handler may not be null.", assertThrows(
                NullPointerException.class,
                () -> new AnnotatedEventHandlingComponent<>(null, PARAMETERS, HANDLERS, typeResolver, converter)
        ).getMessage());
        assertEquals("The MessageTypeResolver may not be null.", assertThrows(
                NullPointerException.class,
                () -> new AnnotatedEventHandlingComponent<>(handlers, PARAMETERS, HANDLERS, null, converter)
        ).getMessage());
        assertEquals("The EventConverter may not be null.", assertThrows(
                NullPointerException.class,
                () -> new AnnotatedEventHandlingComponent<>(handlers, PARAMETERS, HANDLERS, typeResolver, null)
        ).getMessage());
    }

    @Test
    void describesTargetWrappedComponentResolverAndConverter() {
        Handlers handlers = new Handlers();
        AnnotatedEventHandlingComponent<Handlers> component = component(handlers);
        Map<String, Object> properties = new HashMap<>();
        List<Object> wrapped = new java.util.ArrayList<>();
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

        assertSame(handlers, properties.get("target"));
        assertSame(typeResolver, properties.get("messageTypeResolver"));
        assertSame(converter, properties.get("converter"));
        assertEquals(1, wrapped.size());
        assertInstanceOf(org.axonframework.messaging.eventhandling.SimpleEventHandlingComponent.class, wrapped.get(0));
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    @SuppressWarnings("unused")
    private static MessageStream<?> unused() {
        return MessageStream.empty();
    }
}
