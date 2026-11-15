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

import org.axonframework.common.AxonConfigurationException;
import org.axonframework.common.infra.ComponentDescriptor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test class for {@link ConfigurationExtension} interface to achieve 100% coverage.
 */
class ConfigurationExtensionTest {

    @Test
    void nameReturnsTheConfiguredName() {
        TestExtension testSubject = new TestExtension("test-extension");

        String name = testSubject.name();

        assertEquals("test-extension", name);
    }

    @Test
    void validateSucceedsWhenNoExceptionThrown() {
        TestExtension testSubject = new TestExtension("test-extension");

        testSubject.validate();
    }

    @Test
    void validateThrowsAxonConfigurationExceptionWhenInvalid() {
        TestExtension testSubject = new TestExtension("test-extension");
        testSubject.shouldThrow = true;

        assertThrows(AxonConfigurationException.class, testSubject::validate);
    }

    @Test
    void describeToRegistersPropertiesOnDescriptor() {
        TestExtension testSubject = new TestExtension("test-extension");
        RecordingDescriptor descriptor = new RecordingDescriptor();

        testSubject.describeTo(descriptor);

        assertEquals("name", descriptor.lastProperty);
        assertEquals("test-extension", descriptor.lastValue);
    }

    private static class TestExtension implements ConfigurationExtension<ExtendedConfiguration> {

        private final String extensionName;
        boolean shouldThrow;

        TestExtension(String extensionName) {
            this.extensionName = extensionName;
        }

        @Override
        public String name() {
            return extensionName;
        }

        @Override
        public void validate() throws AxonConfigurationException {
            if (shouldThrow) {
                throw new AxonConfigurationException("Invalid extension settings");
            }
        }

        @Override
        public void describeTo(ComponentDescriptor descriptor) {
            descriptor.describeProperty("name", extensionName);
        }
    }

    private static class RecordingDescriptor implements ComponentDescriptor {

        String lastProperty;
        Object lastValue;

        @Override
        public void describeProperty(String name, Object value) {
            this.lastProperty = name;
            this.lastValue = value;
        }

        @Override
        public void describeProperty(String name, java.util.Collection<?> collection) {
            // not used in tests
        }

        @Override
        public void describeProperty(String name, java.util.Map<?, ?> map) {
            // not used in tests
        }

        @Override
        public void describeProperty(String name, String value) {
            this.lastProperty = name;
            this.lastValue = value;
        }

        @Override
        public void describeProperty(String name, Long value) {
            this.lastProperty = name;
            this.lastValue = value;
        }

        @Override
        public void describeProperty(String name, Boolean value) {
            this.lastProperty = name;
            this.lastValue = value;
        }
    }
}