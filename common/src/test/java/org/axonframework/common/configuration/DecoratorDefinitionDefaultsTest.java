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

package org.axonframework.common.configuration;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the lifecycle default methods of the {@link DecoratorDefinition}.
 */
class DecoratorDefinitionDefaultsTest {

    private static final Component.Identifier<String> IDENTIFIER = new Component.Identifier<>(String.class, "cmp");

    private final Configuration configuration = (Configuration) Proxy.newProxyInstance(
            getClass().getClassLoader(), new Class<?>[]{Configuration.class},
            (proxy, method, args) -> {
                throw new UnsupportedOperationException(method.getName());
            });

    private static DecoratorDefinition<String, String> definition() {
        return DecoratorDefinition.forType(String.class).with((config, name, delegate) -> "decorated:" + delegate);
    }

    @Test
    void onStartConsumerIsFluentAndReceivesTheDecoratedComponentInTheGivenPhase() throws Exception {
        AtomicReference<String> received = new AtomicReference<>();
        Consumer<String> handler = received::set;
        DecoratorDefinition<String, String> definition = definition();

        assertSame(definition, definition.onStart(5, handler));
        RecordingRegistry registry = initialize(definition);
        registry.startHandlers.get(0).run(configuration).get(5, TimeUnit.SECONDS);

        assertEquals(List.of(5), registry.startPhases);
        assertEquals("decorated:base", received.get());
    }

    @Test
    void onShutdownConsumerIsFluentAndReceivesTheDecoratedComponentInTheGivenPhase() throws Exception {
        AtomicReference<String> received = new AtomicReference<>();
        Consumer<String> handler = received::set;
        DecoratorDefinition<String, String> definition = definition();

        assertSame(definition, definition.onShutdown(8, handler));
        RecordingRegistry registry = initialize(definition);
        registry.shutdownHandlers.get(0).run(configuration).get(5, TimeUnit.SECONDS);

        assertEquals(List.of(8), registry.shutdownPhases);
        assertEquals("decorated:base", received.get());
    }

    @Test
    void nullHandlersAreRejectedWhenRegisteredInsteadOfWhenTheLifecycleRuns() {
        DecoratorDefinition<String, String> definition = definition();

        assertEquals("The start handler cannot be null.", assertThrows(
                NullPointerException.class, () -> definition.onStart(1, (Consumer<String>) null)).getMessage());
        assertEquals("The shutdown handler cannot be null.", assertThrows(
                NullPointerException.class, () -> definition.onShutdown(1, (Consumer<String>) null)).getMessage());
    }

    @Test
    void forTypeRejectsNullType() {
        assertThrows(NullPointerException.class, () -> DecoratorDefinition.forType(null));
        assertThrows(NullPointerException.class, () -> DecoratorDefinition.forTypeAndName(null, "name"));
    }

    @Test
    void forTypeMatchesAnyNameOfThatTypeButForTypeAndNameOnlyTheGivenName() {
        DecoratorDefinition.CompletedDecoratorDefinition<String, String> byType =
                (DecoratorDefinition.CompletedDecoratorDefinition<String, String>) definition();
        DecoratorDefinition.CompletedDecoratorDefinition<String, String> byTypeAndName =
                (DecoratorDefinition.CompletedDecoratorDefinition<String, String>)
                        DecoratorDefinition.forTypeAndName(String.class, "cmp")
                                           .<String>with((config, name, delegate) -> delegate);

        assertTrue(byType.matches(new Component.Identifier<>(String.class, "anything")));
        assertTrue(byType.matches(new Component.Identifier<>(String.class, null)));
        assertTrue(byTypeAndName.matches(new Component.Identifier<>(String.class, "cmp")));
        assertEquals(false, byTypeAndName.matches(new Component.Identifier<>(String.class, "other")));
        assertEquals(false, byType.matches(new Component.Identifier<>(Integer.class, "cmp")));
    }

    private RecordingRegistry initialize(DecoratorDefinition<String, String> definition) {
        RecordingRegistry registry = new RecordingRegistry();
        ((DecoratorDefinition.CompletedDecoratorDefinition<String, String>) definition)
                .decorate(new InstantiatedComponentDefinition<>(IDENTIFIER, "base"))
                .initLifecycle(configuration, registry);
        return registry;
    }

    private static final class RecordingRegistry implements LifecycleRegistry {

        private final List<Integer> startPhases = new ArrayList<>();
        private final List<LifecycleHandler> startHandlers = new ArrayList<>();
        private final List<Integer> shutdownPhases = new ArrayList<>();
        private final List<LifecycleHandler> shutdownHandlers = new ArrayList<>();

        @Override
        public LifecycleRegistry registerLifecyclePhaseTimeout(long timeout, TimeUnit timeUnit) {
            return this;
        }

        @Override
        public LifecycleRegistry onStart(int phase, LifecycleHandler startHandler) {
            startPhases.add(phase);
            startHandlers.add(startHandler);
            return this;
        }

        @Override
        public LifecycleRegistry onShutdown(int phase, LifecycleHandler shutdownHandler) {
            shutdownPhases.add(phase);
            shutdownHandlers.add(shutdownHandler);
            return this;
        }
    }
}
