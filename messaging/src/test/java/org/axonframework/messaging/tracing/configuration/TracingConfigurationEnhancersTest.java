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

package org.axonframework.messaging.tracing.configuration;

import org.axonframework.common.annotation.RegistrationScope;
import org.axonframework.common.configuration.Configuration;
import org.axonframework.common.configuration.DefaultComponentRegistry;
import org.axonframework.messaging.commandhandling.CommandBus;
import org.axonframework.messaging.commandhandling.tracing.TracingCommandBus;
import org.axonframework.messaging.eventhandling.EventBus;
import org.axonframework.messaging.eventhandling.EventHandlingComponent;
import org.axonframework.messaging.eventhandling.EventSink;
import org.axonframework.messaging.eventhandling.tracing.TracingEventBus;
import org.axonframework.messaging.eventhandling.tracing.TracingEventSink;
import org.axonframework.messaging.queryhandling.QueryBus;
import org.axonframework.messaging.queryhandling.tracing.TracingQueryBus;
import org.axonframework.messaging.tracing.LoggingSpanFactory;
import org.axonframework.messaging.tracing.SpanFactory;
import org.axonframework.messaging.tracing.attributes.DefaultSpanAttributesProviderRegistry;
import org.axonframework.messaging.tracing.attributes.SpanAttributesProviderRegistry;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.function.UnaryOperator;

import static org.axonframework.messaging.tracing.attributes.TracingTestContexts.build;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link MessagingTracingConfigurationEnhancer}, the {@link TracingConfigurationDefaults} and
 * the {@link TracingConfigurationOrder}. Only plain stubs are decorated, so each test shows which decorator wrapped
 * (or deliberately skipped) which component.
 */
class TracingConfigurationEnhancersTest {

    private final CommandBus commandBus = stub(CommandBus.class);
    private final QueryBus queryBus = stub(QueryBus.class);
    private final EventSink eventSink = stub(EventSink.class);
    private final EventBus eventBus = stub(EventBus.class);

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type) {
        return (T) Proxy.newProxyInstance(
                TracingConfigurationEnhancersTest.class.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "equals":
                            return proxy == args[0];
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "toString":
                            return "Stub" + type.getSimpleName();
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
    }

    /**
     * Builds a configuration holding the stubs, with the tracing enhancer applied. A span factory is registered
     * unless the test says otherwise.
     */
    private Configuration configuration(boolean withSpanFactory,
                                        UnaryOperator<MessagingTracingSettings> settingsOverride) {
        DefaultComponentRegistry registry = new DefaultComponentRegistry().disableEnhancerScanning();
        registry.registerComponent(CommandBus.class, c -> commandBus);
        registry.registerComponent(QueryBus.class, c -> queryBus);
        registry.registerComponent(EventSink.class, c -> eventSink);
        registry.registerComponent(EventBus.class, c -> eventBus);
        if (withSpanFactory) {
            registry.registerComponent(SpanFactory.class, c -> LoggingSpanFactory.INSTANCE);
        }
        MessagingTracingSettings settings = settingsOverride.apply(MessagingTracingSettings.enabledByDefault());
        registry.registerComponent(MessagingTracingSettings.class, c -> settings);
        new MessagingTracingConfigurationEnhancer().enhance(registry);
        return build(registry);
    }

    private Configuration configuration() {
        return configuration(true, settings -> settings);
    }

    // --- decorators ---

    @Test
    void wrapsTheCommandBusWhenTracingIsEnabledAndASpanFactoryExists() {
        assertInstanceOf(TracingCommandBus.class, configuration().getComponent(CommandBus.class));
    }

    @Test
    void wrapsTheQueryBusWhenTracingIsEnabledAndASpanFactoryExists() {
        assertInstanceOf(TracingQueryBus.class, configuration().getComponent(QueryBus.class));
    }

    @Test
    void wrapsTheEventSinkWhenTracingIsEnabledAndASpanFactoryExists() {
        assertInstanceOf(TracingEventSink.class, configuration().getComponent(EventSink.class));
    }

    @Test
    void wrapsTheEventBusInAnEventBusNotAnEventSink() {
        assertInstanceOf(TracingEventBus.class, configuration().getComponent(EventBus.class));
    }

    @Test
    void leavesEverythingUntouchedWithoutASpanFactory() {
        Configuration configuration = configuration(false, settings -> settings);

        assertSame(commandBus, configuration.getComponent(CommandBus.class));
        assertSame(queryBus, configuration.getComponent(QueryBus.class));
        assertSame(eventSink, configuration.getComponent(EventSink.class));
        assertSame(eventBus, configuration.getComponent(EventBus.class));
    }

    @Test
    void doesNotWrapTheCommandBusWhenItsTracingIsDisabled() {
        Configuration configuration = configuration(true, settings -> settings.withCommandBusEnabled(false));

        assertSame(commandBus, configuration.getComponent(CommandBus.class));
        assertInstanceOf(TracingQueryBus.class, configuration.getComponent(QueryBus.class));
    }

    @Test
    void doesNotWrapTheQueryBusWhenItsTracingIsDisabled() {
        Configuration configuration = configuration(true, settings -> settings.withQueryBusEnabled(false));

        assertSame(queryBus, configuration.getComponent(QueryBus.class));
        assertInstanceOf(TracingCommandBus.class, configuration.getComponent(CommandBus.class));
    }

    @Test
    void disablingTheEventSinkDisablesTracingOfBothTheSinkAndTheBus() {
        Configuration configuration = configuration(true, settings -> settings.withEventSinkEnabled(false));

        assertSame(eventSink, configuration.getComponent(EventSink.class));
        assertSame(eventBus, configuration.getComponent(EventBus.class));
    }

    @Test
    void anEventBusRegisteredAsAnEventSinkIsLeftToTheEventBusDecorator() {
        DefaultComponentRegistry registry = new DefaultComponentRegistry().disableEnhancerScanning();
        registry.registerComponent(EventSink.class, c -> eventBus);
        registry.registerComponent(SpanFactory.class, c -> LoggingSpanFactory.INSTANCE);
        new MessagingTracingConfigurationEnhancer().enhance(registry);

        assertSame(eventBus, build(registry).getComponent(EventSink.class));
    }

    @Test
    void doesNotTraceEventHandlingComponentsThatAreNotOwnedByAnEventProcessor() {
        EventHandlingComponent component = stub(EventHandlingComponent.class);
        DefaultComponentRegistry registry = new DefaultComponentRegistry().disableEnhancerScanning();
        registry.registerComponent(EventHandlingComponent.class, c -> component);
        registry.registerComponent(SpanFactory.class, c -> LoggingSpanFactory.INSTANCE);
        new MessagingTracingConfigurationEnhancer().enhance(registry);

        assertSame(component, build(registry).getComponent(EventHandlingComponent.class));
    }

    // --- settings default ---

    @Test
    void registersTheEnabledByDefaultSettingsWhenNoneWereRegistered() {
        DefaultComponentRegistry registry = new DefaultComponentRegistry().disableEnhancerScanning();
        new MessagingTracingConfigurationEnhancer().enhance(registry);

        assertEquals(MessagingTracingSettings.enabledByDefault(),
                     build(registry).getComponent(MessagingTracingSettings.class));
    }

    @Test
    void keepsSettingsThatWereRegisteredBeforehand() {
        MessagingTracingSettings custom = MessagingTracingSettings.enabledByDefault().withQueryBusEnabled(false);

        Configuration configuration = configuration(true, settings -> custom);

        assertEquals(custom, configuration.getComponent(MessagingTracingSettings.class));
    }

    // --- orders and annotations ---

    @Test
    void decoratorsAreRegisteredLateButBeforeTheVeryLast() {
        assertEquals(Integer.MAX_VALUE - 1000, TracingConfigurationOrder.TRACING_DECORATOR_ORDER);
        assertEquals(TracingConfigurationOrder.TRACING_DECORATOR_ORDER,
                     MessagingTracingConfigurationEnhancer.TRACING_DECORATOR_ORDER);
        assertTrue(TracingConfigurationOrder.PROVIDER_ENHANCER_ORDER
                           < TracingConfigurationOrder.TRACING_DECORATOR_ORDER);
        assertTrue(TracingConfigurationOrder.TRACING_DECORATOR_ORDER
                           < TracingConfigurationOrder.TRACING_DEFAULTS_ENHANCER_ORDER);
    }

    @Test
    void enhancersRunAtTheDefaultsOrder() {
        assertEquals(TracingConfigurationOrder.TRACING_DEFAULTS_ENHANCER_ORDER,
                     new MessagingTracingConfigurationEnhancer().order());
        assertEquals(TracingConfigurationOrder.TRACING_DEFAULTS_ENHANCER_ORDER,
                     new TracingConfigurationDefaults().order());
    }

    @Test
    void theOrderClassIsANonInstantiableConstantsHolder() {
        Constructor<?>[] constructors = TracingConfigurationOrder.class.getDeclaredConstructors();

        assertTrue(Modifier.isFinal(TracingConfigurationOrder.class.getModifiers()));
        assertEquals(1, constructors.length);
        assertTrue(Modifier.isPrivate(constructors[0].getModifiers()));
    }

    @Test
    void bothEnhancersAreOnlyMeantToBeRegisteredAtTheRoot() {
        assertNotNull(MessagingTracingConfigurationEnhancer.class.getAnnotation(RegistrationScope.class));
        assertNotNull(TracingConfigurationDefaults.class.getAnnotation(RegistrationScope.class));
    }

    // --- tracing defaults ---

    @Test
    void defaultsRegisterTheSpanAttributesProviderRegistryWhenMissing() {
        DefaultComponentRegistry registry = new DefaultComponentRegistry().disableEnhancerScanning();
        new TracingConfigurationDefaults().enhance(registry);

        assertInstanceOf(DefaultSpanAttributesProviderRegistry.class,
                         build(registry).getComponent(SpanAttributesProviderRegistry.class));
    }

    @Test
    void defaultsKeepAnAlreadyRegisteredRegistry() {
        SpanAttributesProviderRegistry custom = new DefaultSpanAttributesProviderRegistry();
        DefaultComponentRegistry registry = new DefaultComponentRegistry().disableEnhancerScanning();
        registry.registerComponent(SpanAttributesProviderRegistry.class, c -> custom);
        new TracingConfigurationDefaults().enhance(registry);

        assertSame(custom, build(registry).getComponent(SpanAttributesProviderRegistry.class));
    }
}
