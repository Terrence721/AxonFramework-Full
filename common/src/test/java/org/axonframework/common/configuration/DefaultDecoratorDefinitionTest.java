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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link DefaultDecoratorDefinition}.
 */
class DefaultDecoratorDefinitionTest {

    private static final Component.Identifier<String> IDENTIFIER = new Component.Identifier<>(String.class, "cmp");

    private static DefaultDecoratorDefinition<String, String> definition(ComponentDecorator<String, String> decorator) {
        return new DefaultDecoratorDefinition<>(id -> true, decorator);
    }

    @Test
    void constructorRejectsNullSelectorAndDecorator() {
        NullPointerException noSelector = assertThrows(
                NullPointerException.class,
                () -> new DefaultDecoratorDefinition<String, String>(null, (config, name, delegate) -> delegate)
        );
        NullPointerException noDecorator = assertThrows(
                NullPointerException.class,
                () -> new DefaultDecoratorDefinition<String, String>(id -> true, null)
        );

        assertEquals("The selector must not be null.", noSelector.getMessage());
        assertEquals("The decorator must not be null.", noDecorator.getMessage());
    }

    @Test
    void matchesDelegatesToTheSelector() {
        DefaultDecoratorDefinition<String, String> testSubject = new DefaultDecoratorDefinition<>(
                id -> "wanted".equals(id.name()), (config, name, delegate) -> delegate
        );

        assertTrue(testSubject.matches(new Component.Identifier<>(String.class, "wanted")));
        assertFalse(testSubject.matches(new Component.Identifier<>(String.class, "other")));
    }

    @Test
    void orderDefaultsToZeroAndCanBeChangedFluently() {
        DefaultDecoratorDefinition<String, String> testSubject = definition((config, name, delegate) -> delegate);

        assertEquals(0, testSubject.order());
        assertSame(testSubject, testSubject.order(7));
        assertEquals(7, testSubject.order());
    }

    @Test
    void lifecycleRegistrationMethodsAreFluent() {
        DefaultDecoratorDefinition<String, String> testSubject = definition((config, name, delegate) -> delegate);

        assertSame(testSubject, testSubject.onStart(1, (configuration, component) -> CompletableFuture.completedFuture(null)));
        assertSame(testSubject, testSubject.onShutdown(1, (configuration, component) -> CompletableFuture.completedFuture(null)));
    }

    @Test
    void decorateKeepsTheIdentifierOfTheDelegate() {
        DefaultDecoratorDefinition<String, String> testSubject = definition((config, name, delegate) -> delegate);

        Component<String> decorated = testSubject.decorate(new InstantiatedComponentDefinition<>(IDENTIFIER, "base"));

        assertEquals(IDENTIFIER, decorated.identifier());
    }

    @Test
    void decoratedComponentAppliesDecoratorWithConfigurationNameAndDelegateValue() {
        AtomicReference<Object[]> received = new AtomicReference<>();
        DefaultDecoratorDefinition<String, String> testSubject = definition((config, name, delegate) -> {
            received.set(new Object[]{config, name, delegate});
            return "decorated:" + delegate;
        });
        Configuration configuration = configuration();

        Component<String> decorated = testSubject.decorate(new InstantiatedComponentDefinition<>(IDENTIFIER, "base"));

        assertEquals("decorated:base", decorated.resolve(configuration));
        assertSame(configuration, received.get()[0]);
        assertEquals("cmp", received.get()[1]);
        assertEquals("base", received.get()[2]);
    }

    @Test
    void decoratedComponentRegistersTheStartAndShutdownHandlersWithTheirPhases() {
        DefaultDecoratorDefinition<String, String> testSubject = definition((config, name, delegate) -> delegate);
        testSubject.onStart(3, (configuration, component) -> CompletableFuture.completedFuture(null));
        testSubject.onShutdown(9, (configuration, component) -> CompletableFuture.completedFuture(null));
        List<String> registrations = new ArrayList<>();
        LifecycleRegistry registry = new LifecycleRegistry() {
            @Override
            public LifecycleRegistry registerLifecyclePhaseTimeout(long timeout, TimeUnit timeUnit) {
                return this;
            }

            @Override
            public LifecycleRegistry onStart(int phase, LifecycleHandler startHandler) {
                registrations.add("start@" + phase);
                return this;
            }

            @Override
            public LifecycleRegistry onShutdown(int phase, LifecycleHandler shutdownHandler) {
                registrations.add("shutdown@" + phase);
                return this;
            }
        };

        Component<String> decorated = testSubject.decorate(new InstantiatedComponentDefinition<>(IDENTIFIER, "base"));
        decorated.initLifecycle(configuration(), registry);

        assertEquals(List.of("start@3", "shutdown@9"), registrations);
    }

    @Test
    void handlersRegisteredAfterDecoratingAreNotPickedUpByEarlierDecoratedComponents() {
        DefaultDecoratorDefinition<String, String> testSubject = definition((config, name, delegate) -> delegate);
        testSubject.onStart(1, (configuration, component) -> CompletableFuture.completedFuture(null));
        Component<String> decorated = testSubject.decorate(new InstantiatedComponentDefinition<>(IDENTIFIER, "base"));
        List<Integer> startPhases = new ArrayList<>();
        LifecycleRegistry registry = new LifecycleRegistry() {
            @Override
            public LifecycleRegistry registerLifecyclePhaseTimeout(long timeout, TimeUnit timeUnit) {
                return this;
            }

            @Override
            public LifecycleRegistry onStart(int phase, LifecycleHandler startHandler) {
                startPhases.add(phase);
                return this;
            }

            @Override
            public LifecycleRegistry onShutdown(int phase, LifecycleHandler shutdownHandler) {
                return this;
            }
        };

        // A decorated component takes a snapshot of the handlers at the time of decorating.
        testSubject.onStart(2, (configuration, component) -> CompletableFuture.completedFuture(null));
        decorated.initLifecycle(configuration(), registry);

        assertEquals(List.of(1), startPhases);
    }

    private static Configuration configuration() {
        return (Configuration) Proxy.newProxyInstance(
                DefaultDecoratorDefinitionTest.class.getClassLoader(),
                new Class<?>[]{Configuration.class},
                (proxy, method, args) -> {
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    if ("toString".equals(method.getName())) {
                        return "TestConfiguration";
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }
}
