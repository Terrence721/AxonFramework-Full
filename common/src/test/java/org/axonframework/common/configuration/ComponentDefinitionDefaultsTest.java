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

import org.axonframework.common.TypeReference;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the static factory methods and default methods of the {@link ComponentDefinition}.
 */
class ComponentDefinitionDefaultsTest {

    private static final TypeReference<List<String>> LIST_OF_STRINGS = new TypeReference<>() {
    };

    private final Configuration configuration = (Configuration) Proxy.newProxyInstance(
            getClass().getClassLoader(), new Class<?>[]{Configuration.class},
            (proxy, method, args) -> {
                throw new UnsupportedOperationException(method.getName());
            });

    // --- static factories ---

    @Test
    void ofTypeCreatesUnnamedDefinitionForTheType() {
        ComponentDefinition<String> definition = ComponentDefinition.ofType(String.class).withInstance("value");

        assertEquals(String.class, definition.rawType());
        assertNull(definition.name());
    }

    @Test
    void ofTypeAndNameCreatesNamedDefinitionForTheType() {
        ComponentDefinition<String> definition =
                ComponentDefinition.ofTypeAndName(String.class, "cmp").withInstance("value");

        assertEquals(String.class, definition.rawType());
        assertEquals("cmp", definition.name());
    }

    @Test
    void ofTypeReferenceCreatesDefinitionWithTheRawType() {
        ComponentDefinition<List<String>> definition =
                ComponentDefinition.ofType(LIST_OF_STRINGS).withInstance(List.of("a"));

        assertEquals(List.class, definition.rawType());
        assertNull(definition.name());
    }

    @Test
    void ofTypeReferenceAndNameCreatesNamedDefinition() {
        ComponentDefinition<List<String>> definition =
                ComponentDefinition.ofTypeAndName(LIST_OF_STRINGS, "names").withInstance(List.of("a"));

        assertEquals(List.class, definition.rawType());
        assertEquals("names", definition.name());
    }

    @Test
    void withInstanceCreatesAnAlreadyInstantiatedComponent() {
        ComponentDefinition<String> definition = ComponentDefinition.ofType(String.class).withInstance("value");

        Component<String> component = componentOf(definition);

        assertTrue(component.isInstantiated());
        assertEquals("value", component.resolve(configuration));
    }

    @Test
    void withBuilderCreatesALazyComponentThatIsNotInstantiatedUntilResolved() {
        ComponentDefinition<String> definition =
                ComponentDefinition.ofType(String.class).withBuilder(config -> "built");

        Component<String> component = componentOf(definition);

        assertFalse(component.isInstantiated());
        assertEquals("built", component.resolve(configuration));
        assertTrue(component.isInstantiated());
    }

    // --- lifecycle defaults ---

    @Test
    void onStartConsumerReceivesTheComponentInTheGivenPhase() throws Exception {
        AtomicReference<String> received = new AtomicReference<>();
        Consumer<String> handler = received::set;
        RecordingRegistry registry = new RecordingRegistry();

        ComponentDefinition<String> definition = ComponentDefinition.ofType(String.class).withInstance("value");
        assertSame(definition, definition.onStart(4, handler));
        initialize(definition, registry);
        registry.startHandlers.get(0).run(configuration).get(5, TimeUnit.SECONDS);

        assertEquals(List.of(4), registry.startPhases);
        assertEquals("value", received.get());
    }

    @Test
    void onStartBiConsumerReceivesConfigurationAndComponent() throws Exception {
        AtomicReference<Object[]> received = new AtomicReference<>();
        BiConsumer<Configuration, String> handler = (config, component) -> received.set(new Object[]{config, component});
        RecordingRegistry registry = new RecordingRegistry();

        ComponentDefinition<String> definition = ComponentDefinition.ofType(String.class).withInstance("value");
        assertSame(definition, definition.onStart(4, handler));
        initialize(definition, registry);
        registry.startHandlers.get(0).run(configuration).get(5, TimeUnit.SECONDS);

        assertSame(configuration, received.get()[0]);
        assertEquals("value", received.get()[1]);
    }

    @Test
    void onShutdownConsumerReceivesTheComponentInTheGivenPhase() throws Exception {
        AtomicReference<String> received = new AtomicReference<>();
        Consumer<String> handler = received::set;
        RecordingRegistry registry = new RecordingRegistry();

        ComponentDefinition<String> definition = ComponentDefinition.ofType(String.class).withInstance("value");
        assertSame(definition, definition.onShutdown(6, handler));
        initialize(definition, registry);
        registry.shutdownHandlers.get(0).run(configuration).get(5, TimeUnit.SECONDS);

        assertEquals(List.of(6), registry.shutdownPhases);
        assertEquals("value", received.get());
    }

    @Test
    void onShutdownBiConsumerReceivesConfigurationAndComponent() throws Exception {
        AtomicReference<Object[]> received = new AtomicReference<>();
        BiConsumer<Configuration, String> handler = (config, component) -> received.set(new Object[]{config, component});
        RecordingRegistry registry = new RecordingRegistry();

        ComponentDefinition<String> definition = ComponentDefinition.ofType(String.class).withInstance("value");
        assertSame(definition, definition.onShutdown(6, handler));
        initialize(definition, registry);
        registry.shutdownHandlers.get(0).run(configuration).get(5, TimeUnit.SECONDS);

        assertSame(configuration, received.get()[0]);
        assertEquals("value", received.get()[1]);
    }

    @Test
    void nullHandlersAreRejectedWhenRegisteredInsteadOfWhenTheLifecycleRuns() {
        ComponentDefinition<String> definition = ComponentDefinition.ofType(String.class).withInstance("value");

        assertEquals("The start handler cannot be null.", assertThrows(
                NullPointerException.class, () -> definition.onStart(1, (Consumer<String>) null)).getMessage());
        assertEquals("The start handler cannot be null.", assertThrows(
                NullPointerException.class,
                () -> definition.onStart(1, (BiConsumer<Configuration, String>) null)).getMessage());
        assertEquals("The shutdown handler cannot be null.", assertThrows(
                NullPointerException.class, () -> definition.onShutdown(1, (Consumer<String>) null)).getMessage());
        assertEquals("The shutdown handler cannot be null.", assertThrows(
                NullPointerException.class,
                () -> definition.onShutdown(1, (BiConsumer<Configuration, String>) null)).getMessage());
    }

    private void initialize(ComponentDefinition<String> definition, LifecycleRegistry registry) {
        componentOf(definition).initLifecycle(configuration, registry);
    }

    @SuppressWarnings("unchecked")
    private static <C> Component<C> componentOf(ComponentDefinition<C> definition) {
        return ((ComponentDefinition.ComponentCreator<C>) definition).createComponent();
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
