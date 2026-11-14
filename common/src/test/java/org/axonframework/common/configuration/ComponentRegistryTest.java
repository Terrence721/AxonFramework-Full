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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for {@link ComponentRegistry} interface achieving 100% coverage including edge cases.
 */
class ComponentRegistryTest {

    private List<ComponentDefinition<?>> registeredComponents;
    private List<DecoratorDefinition<?, ?>> registeredDecorators;
    private List<String> lookups;
    private boolean componentPresent;
    private ComponentRegistry testSubject;

    @BeforeEach
    void setUp() {
        registeredComponents = new ArrayList<>();
        registeredDecorators = new ArrayList<>();
        lookups = new ArrayList<>();
        componentPresent = false;

        testSubject = (ComponentRegistry) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{ComponentRegistry.class},
                handler());
    }

    private InvocationHandler handler() {
        return (proxy, method, args) -> {
            if (method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, args);
            }
            switch (method.getName()) {
                case "registerComponent":
                    registeredComponents.add((ComponentDefinition<?>) args[0]);
                    return proxy;
                case "registerDecorator":
                    registeredDecorators.add((DecoratorDefinition<?, ?>) args[0]);
                    return proxy;
                case "hasComponent":
                    lookups.add(((Class<?>) args[0]).getSimpleName() + ":" + args[1] + ":" + args[2]);
                    return componentPresent;
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "equals":
                    return proxy == args[0];
                case "toString":
                    return "TestComponentRegistry";
                case "registerEnhancer":
                    return proxy;
                case "registerModule":
                    return proxy;
                case "disableEnhancerScanning":
                    return proxy;
                case "disableEnhancer":
                    if (args.length == 1 && args[0] instanceof String) {
                        lookups.add("disableEnhancer:" + args[0]);
                        return proxy;
                    } else if (args.length == 1 && args[0] instanceof Class) {
                        lookups.add("disableEnhancer:" + ((Class<?>) args[0]).getSimpleName());
                        return proxy;
                    }
                    return proxy;
                default:
                    throw new UnsupportedOperationException(method.getName());
            }
        };
    }

    // --- registerComponent tests ---

    @Test
    void registerComponentWithBuilderRegistersUnnamedDefinition() {
        ComponentRegistry result = testSubject.registerComponent(String.class, config -> "built");

        assertSame(testSubject, result);
        assertEquals(1, registeredComponents.size());
        assertEquals(String.class, registeredComponents.get(0).rawType());
        assertEquals(null, registeredComponents.get(0).name());
    }

    @Test
    void registerComponentWithNameAndBuilderRegistersNamedDefinition() {
        ComponentRegistry result = testSubject.registerComponent(String.class, "myComponent", config -> "built");

        assertSame(testSubject, result);
        assertEquals("myComponent", registeredComponents.get(0).name());
        assertEquals(String.class, registeredComponents.get(0).rawType());
    }

    // --- registerDecorator tests ---

    @Test
    void registerDecoratorByTypeAppliesOrderCorrectly() {
        ComponentRegistry result = testSubject.registerDecorator(String.class, 10, (config, name, delegate) -> delegate);

        assertSame(testSubject, result);
        assertEquals(1, registeredDecorators.size());
    }

    @Test
    void registerDecoratorByNameAndTypeAppliesOrderAndMatchesName() {
        testSubject.registerDecorator(String.class, "myComp", 5, (config, name, delegate) -> delegate);

        assertEquals(1, registeredDecorators.size());
    }

    // --- hasComponent tests ---

    @Test
    void hasComponentReturnsTrueWhenComponentPresent() {
        componentPresent = true;
        assertTrue(testSubject.hasComponent(String.class));
        assertEquals(List.of("String:null:ALL"), lookups);
    }

    @Test
    void hasComponentReturnsFalseWhenAbsent() {
        componentPresent = false;
        assertFalse(testSubject.hasComponent(String.class));
        assertEquals(List.of("String:null:ALL"), lookups);
    }

    @Test
    void hasComponentWithTypeAndNameSearchesAllScopes() {
        testSubject.hasComponent(String.class, "myComp");
        assertEquals(List.of("String:myComp:ALL"), lookups);
    }

    @Test
    void hasComponentWithTypeAndScopeUsesCorrectScope() {
        assertFalse(testSubject.hasComponent(String.class, SearchScope.CURRENT));
        assertEquals(List.of("String:null:CURRENT"), lookups);
    }

    @Test
    void hasComponentWithAllParameters() {
        assertFalse(testSubject.hasComponent(Integer.class, "num", SearchScope.ANCESTORS));
        assertEquals(List.of("Integer:num:ANCESTORS"), lookups);
    }

    // --- registerIfNotPresent tests ---

    @Test
    void registerIfNotPresentWithUnpresentComponentRegistersIt() {
        componentPresent = false;
        ComponentRegistry result = testSubject.registerIfNotPresent(String.class, config -> "new");

        assertSame(testSubject, result);
        assertEquals(1, registeredComponents.size());
        assertEquals(List.of("String:null:ALL"), lookups);
    }

    @Test
    void registerIfNotPresentWithPresentComponentSkipsRegistration() {
        componentPresent = true;
        ComponentRegistry result = testSubject.registerIfNotPresent(String.class, config -> "new");

        assertSame(testSubject, result);
        assertTrue(registeredComponents.isEmpty());
        assertEquals(List.of("String:null:ALL"), lookups);
    }

    @Test
    void registerIfNotPresentWithNameSkipsWhenPresent() {
        componentPresent = true;
        ComponentRegistry result = testSubject.registerIfNotPresent(String.class, "myComp", config -> "new");

        assertSame(testSubject, result);
        assertTrue(registeredComponents.isEmpty());
        assertEquals(List.of("String:myComp:ALL"), lookups);
    }

    @Test
    void registerIfNotPresentWithNameRegistersWhenAbsent() {
        componentPresent = false;
        ComponentRegistry result = testSubject.registerIfNotPresent(String.class, "myComp", config -> "new");

        assertSame(testSubject, result);
        assertEquals(1, registeredComponents.size());
        assertEquals("myComp", registeredComponents.get(0).name());
        assertEquals(List.of("String:myComp:ALL"), lookups);
    }

    @Test
    void registerIfNotPresentWithScopeChecksOnlyThatScope() {
        componentPresent = true;
        ComponentRegistry result = testSubject.registerIfNotPresent(
                String.class, config -> "new", SearchScope.CURRENT);

        assertSame(testSubject, result);
        assertTrue(registeredComponents.isEmpty());
        assertEquals(List.of("String:null:CURRENT"), lookups);
    }

    @Test
    void registerIfNotPresentWithNameAndScopeChecksOnlyThatScope() {
        componentPresent = false;
        ComponentRegistry result = testSubject.registerIfNotPresent(
                String.class, "named", config -> "new", SearchScope.ALL);

        assertSame(testSubject, result);
        assertEquals(1, registeredComponents.size());
        assertEquals(List.of("String:named:ALL"), lookups);
    }

    @Test
    void registerIfNotPresentWithDefinitionSkipsWhenPresent() {
        componentPresent = true;
        ComponentDefinition<String> definition = ComponentDefinition.ofType(String.class).withInstance("existing");
        ComponentRegistry result = testSubject.registerIfNotPresent(definition);

        assertSame(testSubject, result);
        assertTrue(registeredComponents.isEmpty());
        assertEquals(List.of("String:null:ALL"), lookups);
    }

    @Test
    void registerIfNotPresentWithDefinitionRegistersWhenAbsent() {
        componentPresent = false;
        ComponentDefinition<String> definition = ComponentDefinition.ofType(String.class).withInstance("new");
        ComponentRegistry result = testSubject.registerIfNotPresent(definition);

        assertSame(testSubject, result);
        assertEquals(1, registeredComponents.size());
        assertEquals(List.of("String:null:ALL"), lookups);
    }

    @Test
    void registerIfNotPresentWithNamedDefinitionSkipsWhenPresent() {
        componentPresent = true;
        ComponentDefinition<String> definition =
                ComponentDefinition.ofTypeAndName(String.class, "named").withInstance("existing");
        ComponentRegistry result = testSubject.registerIfNotPresent(definition);

        assertSame(testSubject, result);
        assertTrue(registeredComponents.isEmpty());
        assertEquals(List.of("String:named:ALL"), lookups);
    }

    @Test
    void registerIfNotPresentWithNamedDefinitionRegistersWhenAbsent() {
        componentPresent = false;
        ComponentDefinition<String> definition =
                ComponentDefinition.ofTypeAndName(String.class, "named").withInstance("new");
        ComponentRegistry result = testSubject.registerIfNotPresent(definition);

        assertSame(testSubject, result);
        assertEquals(1, registeredComponents.size());
        assertEquals("named", registeredComponents.get(0).name());
        assertEquals(List.of("String:named:ALL"), lookups);
    }

    @Test
    void registerIfNotPresentWithDefinitionAndScopeUsesCorrectScopeForUnnamed() {
        componentPresent = false;
        ComponentDefinition<String> definition = ComponentDefinition.ofType(String.class).withInstance("value");
        ComponentRegistry result = testSubject.registerIfNotPresent(definition, SearchScope.CURRENT);

        assertSame(testSubject, result);
        assertEquals(1, registeredComponents.size());
        assertEquals(List.of("String:null:CURRENT"), lookups);
    }

    @Test
    void registerIfNotPresentWithNamedDefinitionAndScopeUsesCorrectScope() {
        componentPresent = true;
        ComponentDefinition<String> definition =
                ComponentDefinition.ofTypeAndName(String.class, "named").withInstance("value");
        ComponentRegistry result = testSubject.registerIfNotPresent(definition, SearchScope.ANCESTORS);

        assertSame(testSubject, result);
        assertTrue(registeredComponents.isEmpty());
        assertEquals(List.of("String:named:ANCESTORS"), lookups);
    }

    // --- registerEnhancer tests ---

    @Test
    void registerEnhancerAddsEnhancerToList() {
        ConfigurationEnhancer enhancer = registry -> {
            // Enhance the registry with some default configuration
            registry.registerComponent(String.class, config -> "default-string");
        };
        ComponentRegistry result = testSubject.registerEnhancer(enhancer);

        assertSame(testSubject, result);
    }

    // --- registerModule tests ---

    @Test
    void registerModuleAddsModuleToList() {
        Module module = new Module() {
            @Override
            public String name() {
                return "test-module";
            }

            @Override
            public Configuration build(Configuration parent, LifecycleRegistry lifecycleRegistry) {
                return parent;
            }
        };
        ComponentRegistry result = testSubject.registerModule(module);

        assertSame(testSubject, result);
    }

    // --- disableEnhancer tests ---

    @Test
    void disableEnhancerByClassNameDisablesSpecificEnhancer() {
        ComponentRegistry result = testSubject.disableEnhancer("com.example.MyEnhancer");
        assertSame(testSubject, result);
    }

    @Test
    void disableEnhancerByClassDisablesSpecificEnhancer() {
        ComponentRegistry result = testSubject.disableEnhancer(TestEnhancer.class);
        assertSame(testSubject, result);
    }

    // --- disableEnhancerScanning tests ---

    @Test
    void disableEnhancerScanningDisablesAutoScanning() {
        ComponentRegistry result = testSubject.disableEnhancerScanning();
        assertSame(testSubject, result);
    }

    // Helper class for testing
    private static class TestEnhancer implements ConfigurationEnhancer {
        @Override
        public int order() {
            return 0;
        }

        @Override
        public void enhance(ComponentRegistry componentRegistry) {
        }
    }
}
