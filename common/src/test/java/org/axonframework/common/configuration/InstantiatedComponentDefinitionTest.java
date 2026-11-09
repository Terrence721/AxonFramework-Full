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

import org.axonframework.common.infra.FilesystemStyleComponentDescriptor;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link InstantiatedComponentDefinition}, complementing the checks in
 * {@link AbstractComponentTest}.
 */
class InstantiatedComponentDefinitionTest {

    private static final Component.Identifier<Object> IDENTIFIER = new Component.Identifier<>(Object.class, "cmp");

    @Test
    void constructorRejectsNullInstanceWithDescriptiveMessage() {
        NullPointerException exception =
                assertThrows(NullPointerException.class, () -> new InstantiatedComponentDefinition<>(IDENTIFIER, null));

        assertEquals("The instance must not be null.", exception.getMessage());
    }

    @Test
    void isInstantiatedBeforeItWasEverResolved() {
        InstantiatedComponentDefinition<Object> testSubject =
                new InstantiatedComponentDefinition<>(IDENTIFIER, new Object());

        assertTrue(testSubject.isInstantiated());
    }

    @Test
    void resolveAlwaysReturnsTheVeryInstanceItWasGiven() {
        Object instance = new Object();
        InstantiatedComponentDefinition<Object> testSubject = new InstantiatedComponentDefinition<>(IDENTIFIER, instance);

        assertSame(instance, testSubject.resolve(configuration()));
        assertSame(instance, testSubject.resolve(configuration()));
    }

    @Test
    void registersNoLifecycleHandlersOnItsOwn() {
        InstantiatedComponentDefinition<Object> testSubject =
                new InstantiatedComponentDefinition<>(IDENTIFIER, new Object());
        List<Integer> phases = new ArrayList<>();
        LifecycleRegistry registry = new LifecycleRegistry() {
            @Override
            public LifecycleRegistry registerLifecyclePhaseTimeout(long timeout, TimeUnit timeUnit) {
                return this;
            }

            @Override
            public LifecycleRegistry onStart(int phase, LifecycleHandler startHandler) {
                phases.add(phase);
                return this;
            }

            @Override
            public LifecycleRegistry onShutdown(int phase, LifecycleHandler shutdownHandler) {
                phases.add(phase);
                return this;
            }
        };

        testSubject.initLifecycle(configuration(), registry);

        assertTrue(phases.isEmpty());
        assertTrue(testSubject.isInitialized());
    }

    @Test
    void describeToReportsIdentifierInstanceAndInstantiatedState() {
        InstantiatedComponentDefinition<Object> testSubject =
                new InstantiatedComponentDefinition<>(IDENTIFIER, "the-instance");
        FilesystemStyleComponentDescriptor descriptor = new FilesystemStyleComponentDescriptor();

        testSubject.describeTo(descriptor);

        String description = descriptor.describe();
        assertTrue(description.contains("identifier"), description);
        assertTrue(description.contains("instantiated"), description);
        assertTrue(description.contains("instance"), description);
        assertTrue(description.contains("the-instance"), description);
    }

    private static Configuration configuration() {
        return (Configuration) Proxy.newProxyInstance(
                InstantiatedComponentDefinitionTest.class.getClassLoader(),
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
