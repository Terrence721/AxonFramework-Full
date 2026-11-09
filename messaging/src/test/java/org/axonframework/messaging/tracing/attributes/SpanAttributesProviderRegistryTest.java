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

package org.axonframework.messaging.tracing.attributes;

import org.axonframework.common.configuration.Configuration;
import org.axonframework.common.configuration.DefaultComponentRegistry;
import org.axonframework.common.infra.FilesystemStyleComponentDescriptor;
import org.axonframework.messaging.tracing.SpanAttributesProvider;
import org.axonframework.messaging.tracing.attributes.configuration.MessagingSpanAttributesProviderConfigurationEnhancer;
import org.axonframework.messaging.tracing.configuration.MessagingTracingSettings;
import org.axonframework.messaging.tracing.configuration.TracingConfigurationOrder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.axonframework.messaging.tracing.attributes.TracingTestContexts.build;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link DefaultSpanAttributesProviderRegistry}, the static
 * {@link SpanAttributesProviderRegistry#register(org.axonframework.common.configuration.ComponentRegistry,
 * org.axonframework.common.configuration.ComponentBuilder)} helper and the
 * {@link MessagingSpanAttributesProviderConfigurationEnhancer}.
 */
class SpanAttributesProviderRegistryTest {

    private static final SpanAttributesProvider FIRST = (message, context) -> Map.of("first", "1");
    private static final SpanAttributesProvider SECOND = (message, context) -> Map.of("second", "2");

    private final DefaultSpanAttributesProviderRegistry testSubject = new DefaultSpanAttributesProviderRegistry();

    private static Configuration emptyConfiguration() {
        return build(new DefaultComponentRegistry().disableEnhancerScanning());
    }

    // --- registry ---

    @Test
    void anEmptyRegistryHasNoProviders() {
        assertTrue(testSubject.providers(emptyConfiguration()).isEmpty());
    }

    @Test
    void providersAreBuiltInRegistrationOrderAgainstTheConfiguration() {
        Configuration configuration = emptyConfiguration();
        List<Configuration> builtWith = new ArrayList<>();

        SpanAttributesProviderRegistry result = testSubject.registerProvider(c -> {
            builtWith.add(c);
            return FIRST;
        }).registerProvider(c -> SECOND);

        assertSame(testSubject, result);
        assertEquals(List.of(FIRST, SECOND), testSubject.providers(configuration));
        assertEquals(List.of(configuration), builtWith);
    }

    @Test
    void describesItsProviderDefinitions() {
        FilesystemStyleComponentDescriptor descriptor = new FilesystemStyleComponentDescriptor();

        testSubject.describeTo(descriptor);

        assertTrue(descriptor.describe().contains("providerDefinitions"), descriptor.describe());
    }

    // --- static register helper ---

    @Test
    void registerContributesAProviderThroughADecoratorOfTheRegistry() {
        DefaultComponentRegistry componentRegistry = new DefaultComponentRegistry().disableEnhancerScanning();
        componentRegistry.registerComponent(SpanAttributesProviderRegistry.class,
                                            c -> new DefaultSpanAttributesProviderRegistry());

        SpanAttributesProviderRegistry.register(componentRegistry, c -> FIRST);
        SpanAttributesProviderRegistry.register(componentRegistry, c -> SECOND);
        Configuration configuration = build(componentRegistry);

        assertEquals(List.of(FIRST, SECOND),
                     configuration.getComponent(SpanAttributesProviderRegistry.class).providers(configuration));
    }

    // --- enhancer ---

    private List<Class<?>> providerTypesFor(MessagingTracingSettings settings) {
        DefaultComponentRegistry componentRegistry = new DefaultComponentRegistry().disableEnhancerScanning();
        componentRegistry.registerComponent(MessagingTracingSettings.class, c -> settings);
        componentRegistry.registerComponent(SpanAttributesProviderRegistry.class,
                                            c -> new DefaultSpanAttributesProviderRegistry());
        new MessagingSpanAttributesProviderConfigurationEnhancer().enhance(componentRegistry);
        Configuration configuration = build(componentRegistry);

        return configuration.getComponent(SpanAttributesProviderRegistry.class)
                            .providers(configuration)
                            .stream()
                            .<Class<?>>map(Object::getClass)
                            .toList();
    }

    @Test
    void enhancerRegistersTheIdTypeAndMetadataProvidersWhenAllAreEnabled() {
        assertEquals(List.of(MessageIdSpanAttributesProvider.class,
                             MessageTypeSpanAttributesProvider.class,
                             MetadataSpanAttributesProvider.class),
                     providerTypesFor(MessagingTracingSettings.enabledByDefault()));
    }

    @Test
    void enhancerOnlyRegistersTheEnabledProviders() {
        MessagingTracingSettings settings = MessagingTracingSettings.enabledByDefault().withSpanAttributesProviders(
                new MessagingTracingSettings.SpanAttributesProviders(false, true, false));

        assertEquals(List.of(MessageTypeSpanAttributesProvider.class), providerTypesFor(settings));
    }

    @Test
    void enhancerRegistersNothingWhenAllProvidersAreDisabled() {
        MessagingTracingSettings settings = MessagingTracingSettings.enabledByDefault().withSpanAttributesProviders(
                new MessagingTracingSettings.SpanAttributesProviders(false, false, false));

        assertTrue(providerTypesFor(settings).isEmpty());
    }

    @Test
    void enhancerRunsInTheProviderEnhancerOrder() {
        assertEquals(TracingConfigurationOrder.PROVIDER_ENHANCER_ORDER,
                     new MessagingSpanAttributesProviderConfigurationEnhancer().order());
    }
}
