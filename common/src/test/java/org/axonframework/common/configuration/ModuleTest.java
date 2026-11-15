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

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for the {@link Module} interface to achieve 100% coverage.
 */
class ModuleTest {

    /**
     * Stub implementation of {@link Module} for testing interface methods.
     */
    private static class TestModule implements Module {

        private final String name;

        TestModule(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Configuration build(Configuration parent, LifecycleRegistry lifecycleRegistry) {
            return parent;
        }
    }

    @Test
    void nameReturnsConfiguredName() {
        Module testSubject = new TestModule("test-module");

        assertSame("test-module", testSubject.name());
    }

    @Test
    void buildReturnsConfigurationFromParent() {
        Module testSubject = new TestModule("test-module");

        Configuration parent = (Configuration) Proxy.newProxyInstance(
                ModuleTest.class.getClassLoader(),
                new Class<?>[]{Configuration.class},
                (proxy, method, args) -> proxy);

        LifecycleRegistry lifecycleRegistry = (LifecycleRegistry) Proxy.newProxyInstance(
                ModuleTest.class.getClassLoader(),
                new Class<?>[]{LifecycleRegistry.class},
                (proxy, method, args) -> proxy);

        Configuration result = testSubject.build(parent, lifecycleRegistry);

        assertSame(parent, result);
    }
}