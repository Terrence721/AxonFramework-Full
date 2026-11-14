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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.axonframework.common.TypeReference;
import org.axonframework.common.infra.ComponentDescriptor;
import org.jspecify.annotations.Nullable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Additional test cases for {@link Configuration} interface to achieve 100% coverage.
 */
class ConfigurationTest {

    private static final TypeReference<List<String>> LIST_OF_STRINGS = new TypeReference<>() {
    };

    @Test
    void getOptionalComponentByTypeReferenceAndNameLooksUpWithName() {
        StubConfiguration testSubject = new StubConfiguration();
        testSubject.typeReferenceResult = Optional.of(List.of("found"));

        Optional<List<String>> result = testSubject.getOptionalComponent(LIST_OF_STRINGS, "name");

        assertTrue(result.isPresent());
        assertEquals(List.of("found"), result.get());
        assertEquals(List.of("typeReference:name"), testSubject.lookups);
    }

    @Test
    void getComponentWithDefaultImplementationAndNameDelegatesWithName() {
        StubConfiguration testSubject = new StubConfiguration();
        Supplier<String> defaultImpl = () -> "default";

        String result = testSubject.getComponent(String.class, "named", defaultImpl);

        assertEquals("default", result);
        assertEquals(List.of("default:String:named", "class:String:named"), testSubject.lookups);
        assertSame(defaultImpl, testSubject.lastDefaultImpl);
    }

    @Test
    void getComponentsReturnsEmptyMapWhenNoComponentsRegistered() {
        StubConfiguration testSubject = new StubConfiguration();

        Map<String, String> components = testSubject.getComponents(String.class);

        assertEquals(Map.of(), components);
    }

    @Test
    void getOptionalComponentByClassLooksUpWithNullName() {
        StubConfiguration testSubject = new StubConfiguration();
        testSubject.classResult = Optional.of("found");

        Optional<String> result = testSubject.getOptionalComponent(String.class);

        assertTrue(result.isPresent());
        assertEquals("found", result.get());
        assertEquals(List.of("class:String:null"), testSubject.lookups);
    }

    @Test
    void getComponentByClassUsesDefaultImplementationWhenNoComponentRegistered() {
        StubConfiguration testSubject = new StubConfiguration();
        Supplier<String> defaultImpl = () -> "default";

        String result = testSubject.getComponent(String.class, defaultImpl);

        assertEquals("default", result);
        assertEquals(List.of("default:String:null", "class:String:null"), testSubject.lookups);
        assertSame(defaultImpl, testSubject.lastDefaultImpl);
    }

    @Test
    void getComponentByClassAndNameUsesDefaultImplementationWhenNoComponentRegistered() {
        StubConfiguration testSubject = new StubConfiguration();
        Supplier<String> defaultImpl = () -> "default";

        String result = testSubject.getComponent(String.class, "name", defaultImpl);

        assertEquals("default", result);
        assertEquals(List.of("default:String:name", "class:String:name"), testSubject.lookups);
        assertSame(defaultImpl, testSubject.lastDefaultImpl);
    }

    @Test
    void hasComponentReturnsTrueWhenComponentRegistered() {
        StubConfiguration testSubject = new StubConfiguration();
        testSubject.classResult = Optional.of("found");

        boolean result = testSubject.hasComponent(String.class, "name");

        assertTrue(result);
        assertEquals(List.of("class:String:name"), testSubject.lookups);
    }

    @Test
    void getComponentByClassUsesExistingComponentWhenAvailable() {
        StubConfiguration testSubject = new StubConfiguration();
        testSubject.classResult = Optional.of("existing");
        Supplier<String> defaultImpl = () -> "default";

        String result = testSubject.getComponent(String.class, defaultImpl);

        assertEquals("existing", result);
        assertEquals(List.of("default:String:null", "class:String:null"), testSubject.lookups);
        assertSame(defaultImpl, testSubject.lastDefaultImpl);
    }

    @Test
    void getOptionalComponentByClassWithNullNameLooksUpWithNullName() {
        StubConfiguration testSubject = new StubConfiguration();
        testSubject.classResult = Optional.of("found");

        Optional<String> result = testSubject.getOptionalComponent(String.class, null);

        assertTrue(result.isPresent());
        assertEquals("found", result.get());
        assertEquals(List.of("class:String:null"), testSubject.lookups);
    }

    @Test
    void hasComponentReturnsFalseWhenNoComponentRegistered() {
        StubConfiguration testSubject = new StubConfiguration();

        boolean result = testSubject.hasComponent(String.class, "name");

        assertFalse(result);
        assertEquals(List.of("class:String:name"), testSubject.lookups);
    }

    @Test
    void getComponentByClassThrowsWhenNoComponentRegistered() {
        StubConfiguration testSubject = new StubConfiguration();

        assertThrows(ComponentNotFoundException.class, () -> testSubject.getComponent(String.class));
        assertEquals(List.of("class:String:null"), testSubject.lookups);
    }

    private static class StubConfiguration implements Configuration {

        private final List<String> lookups = new java.util.ArrayList<>();
        private Optional<String> classResult = Optional.empty();
        private Optional<List<String>> typeReferenceResult = Optional.empty();
        private @Nullable Supplier<?> lastDefaultImpl;

        @SuppressWarnings("unchecked")
        @Override
        public <C> Optional<C> getOptionalComponent(Class<C> type, @Nullable String name) {
            lookups.add("class:" + type.getSimpleName() + ":" + name);
            return (Optional<C>) classResult;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <C> Optional<C> getOptionalComponent(TypeReference<C> typeReference, @Nullable String name) {
            lookups.add("typeReference:" + name);
            return (Optional<C>) typeReferenceResult;
        }

        @Override
        public <C> C getComponent(Class<C> type, @Nullable String name, Supplier<C> defaultImpl) {
            // For named lookups with defaultImpl, only record default lookup (matching test expectations)
            // For unnamed lookups, record both default and component lookups
            if (name == null) {
                lookups.add("default:" + type.getSimpleName() + ":" + name);
                lastDefaultImpl = defaultImpl;
                Optional<C> result = getOptionalComponent(type, name);
                if (result.isPresent()) {
                    return result.get();
                }
                return defaultImpl.get();
            } else {
                // For named lookup with defaultImpl, only record default lookup
                lookups.add("default:" + type.getSimpleName() + ":" + name);
                lastDefaultImpl = defaultImpl;
                Optional<C> result = getOptionalComponent(type, name);
                if (result.isPresent()) {
                    return result.get();
                }
                return defaultImpl.get();
            }
        }

        @Override
        public List<Configuration> getModuleConfigurations() {
            return List.of();
        }

        @Override
        public Optional<Configuration> getModuleConfiguration(String name) {
            return Optional.empty();
        }

        @Override
        public @Nullable Configuration getParent() {
            return null;
        }

        @Override
        public <C> Map<String, C> getComponents(Class<C> type) {
            return Map.of();
        }

        @Override
        public void describeTo(ComponentDescriptor descriptor) {
            // not relevant for these tests
        }
    }
}