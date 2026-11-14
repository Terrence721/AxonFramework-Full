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

package org.axonframework.common.configuration;

import org.axonframework.common.TypeReference;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test class validating the default {@link ApplicationConfigurer#start()} method.
 * Uses anonymous implementations to ensure JaCoCo tracks the default method bytecode execution.
 */
class ApplicationConfigurerDefaultsTest {

    @Test
    void startBuildsTheConfigurationThenStartsItAndReturnsIt() {
        boolean[] started = {false};

        AxonConfiguration mockConfiguration = new AxonConfiguration() {
            @Override
            public void start() {
                started[0] = true;
            }

            @Override
            public void shutdown() {}

            @Override
            public <C> Optional<C> getOptionalComponent(Class<C> type, String name) {
                return Optional.empty();
            }

            @Override
            public <C> Optional<C> getOptionalComponent(TypeReference<C> typeReference, String name) {
                return Optional.empty();
            }

            @Override
            public <C> C getComponent(Class<C> type, String name, Supplier<C> defaultImpl) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<Configuration> getModuleConfigurations() {
                return List.of();
            }

            @Override
            public Optional<Configuration> getModuleConfiguration(String name) {
                return Optional.empty();
            }

            @Override
            public <C> Map<String, C> getComponents(Class<C> type) {
                return Map.of();
            }

            @Override
            public Configuration getParent() {
                return null;
            }

            @Override
            public void describeTo(org.axonframework.common.infra.ComponentDescriptor descriptor) {}
        };

        ApplicationConfigurer configurer = new ApplicationConfigurer() {
            @Override
            public AxonConfiguration build() {
                return mockConfiguration;
            }

            @Override
            public ApplicationConfigurer componentRegistry(Consumer<ComponentRegistry> componentRegistrar) {
                return this;
            }

            @Override
            public ApplicationConfigurer lifecycleRegistry(Consumer<LifecycleRegistry> lifecycleRegistrar) {
                return this;
            }
        };

        AxonConfiguration result = configurer.start();

        assertSame(mockConfiguration, result);
        assertEquals(true, started[0]);
    }

    @Test
    void startDoesNotStartAnythingWhenBuildingFails() {
        IllegalStateException failure = new IllegalStateException("cannot build");

        ApplicationConfigurer configurer = new ApplicationConfigurer() {
            @Override
            public AxonConfiguration build() {
                throw failure;
            }

            @Override
            public ApplicationConfigurer componentRegistry(Consumer<ComponentRegistry> componentRegistrar) {
                return this;
            }

            @Override
            public ApplicationConfigurer lifecycleRegistry(Consumer<LifecycleRegistry> lifecycleRegistrar) {
                return this;
            }
        };

        IllegalStateException thrown = assertThrows(IllegalStateException.class, configurer::start);

        assertSame(failure, thrown);
    }
}
