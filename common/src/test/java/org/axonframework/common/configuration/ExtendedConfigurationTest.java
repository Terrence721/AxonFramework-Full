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

import org.axonframework.common.infra.ComponentDescriptor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Test class for {@link ExtendedConfiguration} interface to achieve 100% coverage.
 */
class ExtendedConfigurationTest {

    @Test
    void extensionReturnsRegisteredExtensionFromImplementation() {
        StubExtendedConfiguration testSubject = new StubExtendedConfiguration();
        RecordingTestExtension expected = testSubject.createRecordingExtension();

        RecordingTestExtension actual = testSubject.extension(RecordingTestExtension.class);

        assertNotNull(actual);
        assertSame(expected, actual);
    }

    @Test
    void extensionReturnsNullWhenNoExtensionRegistered() {
        EmptyExtendedConfiguration testSubject = new EmptyExtendedConfiguration();

        RecordingTestExtension result = testSubject.extension(RecordingTestExtension.class);

        assertNull(result);
    }

    private static class StubExtendedConfiguration implements ExtendedConfiguration {

        private final RecordingTestExtension recordingTestExtension = new RecordingTestExtension();

        @Override
        public <T extends ConfigurationExtension<?>> T extension(Class<T> extensionType) {
            if (extensionType == RecordingTestExtension.class) {
                return extensionType.cast(recordingTestExtension);
            }
            return null;
        }

        private RecordingTestExtension createRecordingExtension() {
            return recordingTestExtension;
        }
    }

    private static class EmptyExtendedConfiguration implements ExtendedConfiguration {

        @Override
        public <T extends ConfigurationExtension<?>> T extension(Class<T> extensionType) {
            return null;
        }
    }

    private static class RecordingTestExtension implements ConfigurationExtension<ExtendedConfiguration> {

        @Override
        public String name() {
            return "recording";
        }

        @Override
        public void validate() {
        }

        @Override
        public void describeTo(ComponentDescriptor descriptor) {
            descriptor.describeProperty("extension", this);
        }
    }
}
