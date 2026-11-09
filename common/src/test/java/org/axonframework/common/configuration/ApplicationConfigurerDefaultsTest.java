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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test class validating the default {@link ApplicationConfigurer#start()}.
 */
class ApplicationConfigurerDefaultsTest {

    private final List<String> events = new ArrayList<>();

    private final AxonConfiguration configuration = (AxonConfiguration) Proxy.newProxyInstance(
            getClass().getClassLoader(), new Class<?>[]{AxonConfiguration.class},
            (proxy, method, args) -> {
                switch (method.getName()) {
                    case "start":
                        events.add("start");
                        return null;
                    case "hashCode":
                        return System.identityHashCode(proxy);
                    case "equals":
                        return proxy == args[0];
                    case "toString":
                        return "TestConfiguration";
                    default:
                        throw new UnsupportedOperationException(method.getName());
                }
            });

    /**
     * An {@link ApplicationConfigurer} that runs the real default methods and only intercepts {@code build()}.
     */
    private ApplicationConfigurer configurerBuilding(java.util.function.Supplier<AxonConfiguration> build) {
        return (ApplicationConfigurer) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{ApplicationConfigurer.class},
                (proxy, method, args) -> {
                    if (method.isDefault()) {
                        return InvocationHandler.invokeDefault(proxy, method, args);
                    }
                    if ("build".equals(method.getName())) {
                        return build.get();
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    @Test
    void startBuildsTheConfigurationThenStartsItAndReturnsIt() {
        ApplicationConfigurer testSubject = configurerBuilding(() -> {
            events.add("build");
            return configuration;
        });

        AxonConfiguration result = testSubject.start();

        assertSame(configuration, result);
        assertEquals(List.of("build", "start"), events);
    }

    @Test
    void startDoesNotStartAnythingWhenBuildingFails() {
        IllegalStateException failure = new IllegalStateException("cannot build");
        ApplicationConfigurer testSubject = configurerBuilding(() -> {
            throw failure;
        });

        IllegalStateException thrown = assertThrows(IllegalStateException.class, testSubject::start);

        assertSame(failure, thrown);
        assertEquals(List.of(), events);
    }
}
