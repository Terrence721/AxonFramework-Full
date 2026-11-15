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

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Test class for {@link ExtensibleConfigurer} interface to achieve 100% coverage.
 */
class ExtensibleConfigurerTest {

    /**
     * Concrete implementation that records the parameters passed to {@link ExtensibleConfigurer#extend(Class, Supplier)}.
     */
    private static class RecordingExtensibleConfigurer implements ExtensibleConfigurer {

        Class<? extends ConfigurationExtension<?>> recordedExtensionClass;
        Supplier<? extends ConfigurationExtension<?>> recordedFactory;

        @Override
        public <E extends ConfigurationExtension<?>> ExtensibleConfigurer extend(Class<E> extensionClass,
                                                                                  Supplier<? extends E> extensionFactory) {
            this.recordedExtensionClass = extensionClass;
            this.recordedFactory = extensionFactory;
            return this;
        }
    }

    /**
     * Minimal {@link ConfigurationExtension} used as a registration payload.
     */
    private static class TestExtension implements ConfigurationExtension<ExtendedConfiguration> {

        @Override
        public String name() {
            return "test";
        }

        @Override
        public void validate() {
        }

        @Override
        public void describeTo(org.axonframework.common.infra.ComponentDescriptor descriptor) {
            descriptor.describeProperty("test", this);
        }
    }

    @Test
    void extendWithInstanceDelegatesToExtendWithClassAndSupplier() {
        RecordingExtensibleConfigurer testSubject = new RecordingExtensibleConfigurer();
        TestExtension extension = new TestExtension();

        ExtensibleConfigurer result = testSubject.extend(extension);

        assertSame(testSubject, result);
        assertSame(TestExtension.class, testSubject.recordedExtensionClass);
        assertSame(extension, testSubject.recordedFactory.get());
    }
}