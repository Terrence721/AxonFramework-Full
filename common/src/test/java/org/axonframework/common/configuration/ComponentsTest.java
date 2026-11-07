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
 * See the License for the specific language governing permissions and limitations
 * under the License.
 */

package org.axonframework.common.configuration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentsTest {

    static class TestComponent implements Component<String> {
        @Override
        public Component.Identifier<String> identifier() {
            return new Component.Identifier<>(String.class, null);
        }

        @Override
        public String resolve(Configuration configuration) {
            return "value";
        }

        @Override
        public boolean isInstantiated() {
            return true;
        }

        @Override
        public void initLifecycle(Configuration configuration, LifecycleRegistry lifecycleRegistry) {
        }

        @Override
        public boolean isInitialized() {
            return false;
        }

        @Override
        public void describeTo(org.axonframework.common.infra.ComponentDescriptor descriptor) {
        }
    }

    @Test
    void putAndGetReturnsRegisteredComponent() {
        Components components = new Components();
        TestComponent component = new TestComponent();

        components.put(component);

        assertTrue(components.get(new Component.Identifier<>(String.class, null)).isPresent());
        assertEquals(component, components.get(new Component.Identifier<>(String.class, null)).get());
    }

    @Test
    void containsReturnsTrueAfterPut() {
        Components components = new Components();
        components.put(new TestComponent());

        assertTrue(components.contains(new Component.Identifier<>(String.class, null)));
    }
}
