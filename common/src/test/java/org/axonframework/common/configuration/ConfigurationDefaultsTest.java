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

import org.axonframework.common.TypeReference;
import org.axonframework.common.infra.ComponentDescriptor;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the default methods of the {@link Configuration}, through a stub that only implements the
 * abstract lookups and records how they were invoked.
 */
class ConfigurationDefaultsTest {

    private static final TypeReference<List<String>> LIST_OF_STRINGS = new TypeReference<>() {
    };

    private final StubConfiguration testSubject = new StubConfiguration();

    @Test
    void getComponentByClassLooksUpWithoutName() {
        testSubject.classResult = Optional.of("found");

        assertEquals("found", testSubject.getComponent(String.class));
        assertEquals(List.of("class:String:null"), testSubject.lookups);
    }

    @Test
    void getComponentByClassAndNamePassesTheNameAlong() {
        testSubject.classResult = Optional.of("found");

        assertEquals("found", testSubject.getComponent(String.class, "name"));
        assertEquals(List.of("class:String:name"), testSubject.lookups);
    }

    @Test
    void getComponentByClassThrowsComponentNotFoundExceptionWhenAbsent() {
        ComponentNotFoundException exception =
                assertThrows(ComponentNotFoundException.class, () -> testSubject.getComponent(String.class, "name"));

        assertEquals("No component found for type [class java.lang.String] name [name].", exception.getMessage());
    }

    @Test
    void getOptionalComponentByClassLooksUpWithoutName() {
        testSubject.classResult = Optional.of("found");

        assertEquals(Optional.of("found"), testSubject.getOptionalComponent(String.class));
        assertEquals(List.of("class:String:null"), testSubject.lookups);
    }

    @Test
    void getComponentByTypeReferenceLooksUpWithoutName() {
        List<String> list = List.of("a");
        testSubject.typeReferenceResult = Optional.of(list);

        assertSame(list, testSubject.getComponent(LIST_OF_STRINGS));
        assertEquals(List.of("typeReference:null"), testSubject.lookups);
    }

    @Test
    void getComponentByTypeReferenceAndNamePassesTheNameAlong() {
        testSubject.typeReferenceResult = Optional.of(List.of("a"));

        testSubject.getComponent(LIST_OF_STRINGS, "name");

        assertEquals(List.of("typeReference:name"), testSubject.lookups);
    }

    @Test
    void getComponentByTypeReferenceThrowsComponentNotFoundExceptionWhenAbsent() {
        assertThrows(ComponentNotFoundException.class, () -> testSubject.getComponent(LIST_OF_STRINGS, "name"));
    }

    @Test
    void getOptionalComponentByTypeReferenceLooksUpWithoutName() {
        testSubject.typeReferenceResult = Optional.of(List.of("a"));

        assertTrue(testSubject.getOptionalComponent(LIST_OF_STRINGS).isPresent());
        assertEquals(List.of("typeReference:null"), testSubject.lookups);
    }

    @Test
    void hasComponentByClassReflectsPresenceOfTheComponent() {
        assertFalse(testSubject.hasComponent(String.class));

        testSubject.classResult = Optional.of("found");

        assertTrue(testSubject.hasComponent(String.class));
        assertTrue(testSubject.hasComponent(String.class, "name"));
        assertEquals(List.of("class:String:null", "class:String:null", "class:String:name"), testSubject.lookups);
    }

    @Test
    void hasComponentByTypeReferenceReflectsPresenceOfTheComponent() {
        assertFalse(testSubject.hasComponent(LIST_OF_STRINGS));

        testSubject.typeReferenceResult = Optional.of(List.of("a"));

        assertTrue(testSubject.hasComponent(LIST_OF_STRINGS));
        assertTrue(testSubject.hasComponent(LIST_OF_STRINGS, "name"));
        assertEquals(List.of("typeReference:null", "typeReference:null", "typeReference:name"), testSubject.lookups);
    }

    @Test
    void getComponentWithDefaultImplementationDelegatesWithoutName() {
        Supplier<String> defaultImpl = () -> "default";

        assertEquals("default", testSubject.getComponent(String.class, defaultImpl));
        assertEquals(List.of("default:String:null"), testSubject.lookups);
        assertSame(defaultImpl, testSubject.lastDefaultImpl);
    }

    /**
     * Implements only the abstract lookups of the {@link Configuration}, recording each invocation.
     */
    private static final class StubConfiguration implements Configuration {

        private final List<String> lookups = new ArrayList<>();
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
            lookups.add("default:" + type.getSimpleName() + ":" + name);
            lastDefaultImpl = defaultImpl;
            return defaultImpl.get();
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
