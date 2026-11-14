/*
 * Copyright (c) 2010-2026. Axon Framework
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
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for {@link ComponentFactory} interface to achieve 100% coverage.
 */
class ComponentFactoryTest {

    /**
     * Concrete implementation of ComponentFactory for testing purposes.
     */
    private static class TestableComponentFactory implements ComponentFactory<String> {

        private final List<String> calls = new java.util.ArrayList<>();

        @Override
        public Class<String> forType() {
            return String.class;
        }

        @Override
        public Optional<Component<String>> construct(String name, Configuration config) {
            calls.add("construct:" + name);
            return Optional.empty();
        }

        @Override
        public void registerShutdownHandlers(LifecycleRegistry registry) {
            calls.add("registerShutdownHandlers");
            registry.onShutdown(() -> {});
        }

        @Override
        public void describeTo(org.axonframework.common.infra.ComponentDescriptor descriptor) {
            descriptor.describeProperty("factoryName", "test-factory");
            descriptor.describeWrapperOf(List.of("item1", "item2"));
        }

        public List<String> getCalls() {
            return new java.util.ArrayList<>(calls);
        }
    }

    @Test
    void forTypeReturnsStringClass() {
        ComponentFactory<String> factory = new TestableComponentFactory();

        assertEquals(String.class, factory.forType());
    }

    @Test
    void constructRejectsUnknownNames() {
        ComponentFactory<String> factory = new TestableComponentFactory();

        Configuration mockConfig = new Configuration() {
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
            public Configuration getParent() {
                return null;
            }

            @Override
            public <C> Map<String, C> getComponents(Class<C> type) {
                return Map.of();
            }

            @Override
            public void describeTo(org.axonframework.common.infra.ComponentDescriptor descriptor) {}
        };

        Optional<Component<String>> result = factory.construct("unknown", mockConfig);

        assertTrue(result.isEmpty());
    }

    @Test
    void constructRecordsCallWithName() {
        TestableComponentFactory factory = new TestableComponentFactory();
        Configuration mockConfig = new Configuration() {
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
            public Configuration getParent() {
                return null;
            }

            @Override
            public <C> Map<String, C> getComponents(Class<C> type) {
                return Map.of();
            }

            @Override
            public void describeTo(org.axonframework.common.infra.ComponentDescriptor descriptor) {}
        };

        factory.construct("myName", mockConfig);

        assertEquals(1, factory.getCalls().size());
        assertEquals("construct:myName", factory.getCalls().get(0));
    }

    @Test
    void registerShutdownHandlersRegistersHandlerWithLifecycle() {
        TestableComponentFactory factory = new TestableComponentFactory();
        boolean[] handlerCalled = {false};

        LifecycleRegistry registry = new LifecycleRegistry() {
            @Override
            public LifecycleRegistry registerLifecyclePhaseTimeout(long timeout, TimeUnit timeUnit) {
                return this;
            }

            @Override
            public LifecycleRegistry onStart(int phase, LifecycleHandler startHandler) {
                return this;
            }

            @Override
            public LifecycleRegistry onShutdown(int phase, LifecycleHandler shutdownHandler) {
                shutdownHandler.run(null).join();
                handlerCalled[0] = true;
                return this;
            }
        };

        factory.registerShutdownHandlers(registry);

        assertTrue(factory.getCalls().contains("registerShutdownHandlers"));
        assertTrue(handlerCalled[0]);
    }

    @Test
    void describeToDescribesNameAndCollection() {
        TestableComponentFactory factory = new TestableComponentFactory();
        StringBuilder sb = new StringBuilder();
        List<String> properties = new java.util.ArrayList<>();

        org.axonframework.common.infra.ComponentDescriptor descriptor = new org.axonframework.common.infra.ComponentDescriptor() {
            @Override
            public void describeProperty(String name, Object object) {
                properties.add("desc:" + name + "=" + object);
            }

            @Override
            public void describeProperty(String name, java.util.Collection<?> collection) {
                properties.add("desc:" + name + "=" + collection);
            }

            @Override
            public void describeProperty(String name, Map<?, ?> map) {
                properties.add("desc:" + name + "=" + map);
            }

            @Override
            public void describeProperty(String name, String value) {
                properties.add("desc:" + name + "=" + value);
            }

            @Override
            public void describeProperty(String name, Long value) {
                properties.add("desc:" + name + "=" + value);
            }

            @Override
            public void describeProperty(String name, Boolean value) {
                properties.add("desc:" + name + "=" + value);
            }
        };

        factory.describeTo(descriptor);

        assertTrue(properties.stream().anyMatch(p -> p.contains("desc:factoryName=test-factory")));
        assertTrue(properties.stream().anyMatch(p -> p.contains("desc:delegate=[item1, item2]")));
    }

    @Test
    void resolvedTypeNameReturnsClassNameWhenNotAComponent() {
        ComponentFactory<String> factory = new TestableComponentFactory();

        String typeName = org.axonframework.common.infra.DescribableComponent.resolvedTypeName(factory);

        assertTrue(typeName.endsWith("TestableComponentFactory"));
    }

    @Test
    void componentFactoryForTypeReturnsClass() {
        // Verify the factory's identifier reflects its generic type
        assertEquals(String.class, new TestableComponentFactory().forType());
    }
}
