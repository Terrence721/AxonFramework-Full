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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the default methods of the {@link ComponentRegistry}. The registry under test is a proxy that
 * runs the real default methods and only intercepts the three abstract operations they delegate to, recording how they
 * were invoked.
 */
class ComponentRegistryDefaultsTest {

    private final List<ComponentDefinition<?>> registeredComponents = new ArrayList<>();
    private final List<DecoratorDefinition<?, ?>> registeredDecorators = new ArrayList<>();
    private final List<String> lookups = new ArrayList<>();
    private boolean componentPresent;

    private final ComponentRegistry testSubject = (ComponentRegistry) Proxy.newProxyInstance(
            getClass().getClassLoader(), new Class<?>[]{ComponentRegistry.class}, handler());

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
                default:
                    throw new UnsupportedOperationException(method.getName());
            }
        };
    }

    // --- registerComponent ---

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
        testSubject.registerComponent(String.class, "cmp", config -> "built");

        assertEquals("cmp", registeredComponents.get(0).name());
        assertEquals(String.class, registeredComponents.get(0).rawType());
    }

    // --- registerDecorator ---

    @Test
    void registerDecoratorByTypeAppliesOrderAndMatchesAnyNameOfThatType() {
        ComponentRegistry result = testSubject.registerDecorator(String.class, 12, (config, name, delegate) -> delegate);

        assertSame(testSubject, result);
        DecoratorDefinition.CompletedDecoratorDefinition<?, ?> definition = completed(registeredDecorators.get(0));
        assertEquals(12, definition.order());
        assertTrue(definition.matches(new Component.Identifier<>(String.class, "any")));
        assertFalse(definition.matches(new Component.Identifier<>(Integer.class, "any")));
    }

    @Test
    void registerDecoratorByTypeAndNameOnlyMatchesThatName() {
        testSubject.registerDecorator(String.class, "cmp", 3, (config, name, delegate) -> delegate);

        DecoratorDefinition.CompletedDecoratorDefinition<?, ?> definition = completed(registeredDecorators.get(0));
        assertEquals(3, definition.order());
        assertTrue(definition.matches(new Component.Identifier<>(String.class, "cmp")));
        assertFalse(definition.matches(new Component.Identifier<>(String.class, "other")));
    }

    // --- hasComponent ---

    @Test
    void hasComponentByTypeSearchesUnnamedInAllScopes() {
        componentPresent = true;

        assertTrue(testSubject.hasComponent(String.class));
        assertEquals(List.of("String:null:ALL"), lookups);
    }

    @Test
    void hasComponentByTypeAndScopeSearchesUnnamedInThatScope() {
        assertFalse(testSubject.hasComponent(String.class, SearchScope.ANCESTORS));
        assertEquals(List.of("String:null:ANCESTORS"), lookups);
    }

    @Test
    void hasComponentByTypeAndNameSearchesAllScopes() {
        testSubject.hasComponent(String.class, "cmp");
        assertEquals(List.of("String:cmp:ALL"), lookups);
    }

    // --- registerIfNotPresent ---

    @Test
    void registerIfNotPresentRegistersWhenAbsent() {
        ComponentRegistry result = testSubject.registerIfNotPresent(String.class, config -> "built");

        assertSame(testSubject, result);
        assertEquals(List.of("String:null:ALL"), lookups);
        assertEquals(1, registeredComponents.size());
        assertEquals(null, registeredComponents.get(0).name());
    }

    @Test
    void registerIfNotPresentSkipsRegistrationWhenPresent() {
        componentPresent = true;

        ComponentRegistry result = testSubject.registerIfNotPresent(String.class, config -> "built");

        assertSame(testSubject, result);
        assertTrue(registeredComponents.isEmpty());
    }

    @Test
    void registerIfNotPresentWithScopePassesTheScopeToTheLookup() {
        testSubject.registerIfNotPresent(String.class, config -> "built", SearchScope.CURRENT);

        assertEquals(List.of("String:null:CURRENT"), lookups);
        assertEquals(1, registeredComponents.size());
    }

    @Test
    void registerIfNotPresentWithNameLooksUpAndRegistersTheNamedComponent() {
        testSubject.registerIfNotPresent(String.class, "cmp", config -> "built");

        assertEquals(List.of("String:cmp:ALL"), lookups);
        assertEquals("cmp", registeredComponents.get(0).name());
    }

    @Test
    void registerIfNotPresentWithNameAndScopePassesBoth() {
        testSubject.registerIfNotPresent(String.class, "cmp", config -> "built", SearchScope.ANCESTORS);

        assertEquals(List.of("String:cmp:ANCESTORS"), lookups);
        assertEquals("cmp", registeredComponents.get(0).name());
    }

    @Test
    void registerIfNotPresentWithNamedDefinitionOnlyChecksThatName() {
        ComponentDefinition<String> definition =
                ComponentDefinition.ofTypeAndName(String.class, "cmp").withInstance("value");

        testSubject.registerIfNotPresent(definition);

        assertEquals(List.of("String:cmp:ALL"), lookups);
        assertSame(definition, registeredComponents.get(0));
    }

    @Test
    void registerIfNotPresentWithUnnamedDefinitionChecksTheTypeOnly() {
        ComponentDefinition<String> definition = ComponentDefinition.ofType(String.class).withInstance("value");

        testSubject.registerIfNotPresent(definition);

        assertEquals(List.of("String:null:ALL"), lookups);
        assertSame(definition, registeredComponents.get(0));
    }

    @Test
    void registerIfNotPresentWithDefinitionAndScopeUsesTheScopeForNamedAndUnnamed() {
        testSubject.registerIfNotPresent(ComponentDefinition.ofType(String.class).withInstance("a"),
                                         SearchScope.CURRENT);
        testSubject.registerIfNotPresent(ComponentDefinition.ofTypeAndName(String.class, "cmp").withInstance("b"),
                                         SearchScope.ANCESTORS);

        assertEquals(List.of("String:null:CURRENT", "String:cmp:ANCESTORS"), lookups);
        assertEquals(2, registeredComponents.size());
    }

    @Test
    void registerIfNotPresentWithDefinitionSkipsRegistrationWhenPresent() {
        componentPresent = true;

        ComponentRegistry result = testSubject.registerIfNotPresent(
                ComponentDefinition.ofType(String.class).withInstance("value"), SearchScope.ALL);

        assertSame(testSubject, result);
        assertTrue(registeredComponents.isEmpty());
    }

    private static DecoratorDefinition.CompletedDecoratorDefinition<?, ?> completed(DecoratorDefinition<?, ?> definition) {
        return (DecoratorDefinition.CompletedDecoratorDefinition<?, ?>) definition;
    }
}
