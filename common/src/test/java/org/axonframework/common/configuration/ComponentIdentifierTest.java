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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.common.configuration;

import org.axonframework.common.TypeReference;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link Component.Identifier} record.
 */
class ComponentIdentifierTest {

    @Test
    void validatesIdentifierInputs() {
        assertThrows(NullPointerException.class,
            () -> new org.axonframework.common.configuration.Component.Identifier<String>((Class<String>) null, "name"));
        assertThrows(IllegalArgumentException.class,
                () -> new org.axonframework.common.configuration.Component.Identifier<>(String.class, ""));
    }

    @Test
    void matchesByRawTypeAndName() {
        org.axonframework.common.configuration.Component.Identifier<Object> first =
                new org.axonframework.common.configuration.Component.Identifier<>(Object.class, "cmp");
        org.axonframework.common.configuration.Component.Identifier<String> second =
                new org.axonframework.common.configuration.Component.Identifier<>(String.class, "cmp");

        assertTrue(first.matches(second));
        assertFalse(first.matches(new org.axonframework.common.configuration.Component.Identifier<>(String.class, "other")));
    }

    @Test
    void matchesByTypeReference() {
        TypeReference<java.util.List<String>> typeRef =
                new TypeReference<java.util.List<String>>() {};
        org.axonframework.common.configuration.Component.Identifier<java.util.List<String>> first =
                new org.axonframework.common.configuration.Component.Identifier<>(typeRef, "name");
        org.axonframework.common.configuration.Component.Identifier<java.util.List<String>> second = first;

        assertTrue(first.matchesByTypeRef(second));
        assertEquals(java.util.List.class, first.typeAsClass());
        assertTrue(first.matchesByTypeRef(first));
    }

    @Test
    void exposesTypeAsClassAndEquality() {
        org.axonframework.common.configuration.Component.Identifier<String> first =
                new org.axonframework.common.configuration.Component.Identifier<>(String.class, "cmp");
        org.axonframework.common.configuration.Component.Identifier<String> second =
                new org.axonframework.common.configuration.Component.Identifier<>(String.class, "cmp");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void areTypeAndNameEqualWhenTypeNameMatchesIdentifierName() {
        org.axonframework.common.configuration.Component.Identifier<String> identifier =
                new org.axonframework.common.configuration.Component.Identifier<>(String.class, "java.lang.String");

        assertTrue(identifier.areTypeAndNameEqual());
    }

    @Test
    void areTypeAndNameNotEqualWhenTypeNameDiffersFromIdentifierName() {
        org.axonframework.common.configuration.Component.Identifier<String> identifier =
                new org.axonframework.common.configuration.Component.Identifier<>(String.class, "different-name");

        assertFalse(identifier.areTypeAndNameEqual());
    }

    @Test
    void toStringReturnsFormattedIdentifier() {
        org.axonframework.common.configuration.Component.Identifier<String> identifier =
                new org.axonframework.common.configuration.Component.Identifier<>(String.class, "my-component");

        assertEquals("java.lang.String:my-component", identifier.toString());
    }

    @Test
    void constructorWithTypeAcceptsNullName() {
        // This should succeed when type is valid and name is null
        java.lang.reflect.Type type = String.class;
        @SuppressWarnings("unchecked")
        org.axonframework.common.configuration.Component.Identifier<Object> identifier =
                new org.axonframework.common.configuration.Component.Identifier<>((Type) type, null);

        assertEquals(String.class, identifier.typeAsClass());
        assertEquals(null, identifier.name());
    }

    @Test
    void constructorWithTypeRejectsNullType() {
        assertThrows(NullPointerException.class, () -> {
            new org.axonframework.common.configuration.Component.Identifier<>((Type) null, "name");
        });
    }

    @Test
    void constructorWithTypeRejectsEmptyName() {
        assertThrows(IllegalArgumentException.class, () -> {
            new org.axonframework.common.configuration.Component.Identifier<>((Type) String.class, "");
        });
    }

    @Test
    void constructorWithTypeReferenceAllowsNullName() {
        // Test that null name is allowed through TypeReference constructor
        TypeReference<String> typeRef = new TypeReference<String>() {};
        org.axonframework.common.configuration.Component.Identifier<String> identifier =
                new org.axonframework.common.configuration.Component.Identifier<>(typeRef, null);

        assertEquals(typeRef, identifier.type());
        assertEquals(null, identifier.name());
    }

    @Test
    void constructorWithTypeReferenceRejectsNullType() {
        assertThrows(NullPointerException.class, () -> {
            new org.axonframework.common.configuration.Component.Identifier<>((TypeReference<String>) null, "name");
        });
    }

    @Test
    void constructorWithTypeReferenceRejectsEmptyName() {
        TypeReference<String> typeRef = new TypeReference<String>() {};
        assertThrows(IllegalArgumentException.class, () -> {
            new org.axonframework.common.configuration.Component.Identifier<>(typeRef, "");
        });
    }

    @Test
    void matchesTypeByRawTypeChecksAssignableFrom() {
        org.axonframework.common.configuration.Component.Identifier<CharSequence> first =
                new org.axonframework.common.configuration.Component.Identifier<>(CharSequence.class, "cmp");
        org.axonframework.common.configuration.Component.Identifier<String> second =
                new org.axonframework.common.configuration.Component.Identifier<>(String.class, "cmp");

        assertTrue(first.matchesType(second));
        assertFalse(second.matchesType(first)); // String is not assignable from CharSequence
    }

    @Test
    void matchesTypeByTypeRefChecksAssignableFrom() {
        TypeReference<CharSequence> charSeqType = new TypeReference<CharSequence>() {};
        TypeReference<String> stringType = new TypeReference<String>() {};

        org.axonframework.common.configuration.Component.Identifier<CharSequence> first =
                new org.axonframework.common.configuration.Component.Identifier<>(charSeqType, "cmp");
        org.axonframework.common.configuration.Component.Identifier<String> second =
                new org.axonframework.common.configuration.Component.Identifier<>(stringType, "cmp");

        assertTrue(first.matchesTypeByTypeRef(second));
        assertFalse(second.matchesTypeByTypeRef(first)); // String is not assignable from CharSequence
    }

    @Test
    void matchesByTypeRefWithDifferentNamesReturnsFalse() {
        TypeReference<String> typeRef = new TypeReference<String>() {};
        org.axonframework.common.configuration.Component.Identifier<String> first =
                new org.axonframework.common.configuration.Component.Identifier<>(typeRef, "name1");
        org.axonframework.common.configuration.Component.Identifier<String> second =
                new org.axonframework.common.configuration.Component.Identifier<>(typeRef, "name2");

        assertFalse(first.matchesByTypeRef(second));
    }

    @Test
    void matchesByNameEqualityWhenTypesAreCompatible() {
        org.axonframework.common.configuration.Component.Identifier<Object> first =
                new org.axonframework.common.configuration.Component.Identifier<>(Object.class, "same-name");
        org.axonframework.common.configuration.Component.Identifier<String> second =
                new org.axonframework.common.configuration.Component.Identifier<>(String.class, "same-name");

        assertTrue(first.matches(second));
    }

    @Test
    void matchesWithIncompatibleTypesReturnsFalse() {
        org.axonframework.common.configuration.Component.Identifier<String> first =
                new org.axonframework.common.configuration.Component.Identifier<>(String.class, "cmp");
        org.axonframework.common.configuration.Component.Identifier<Integer> second =
                new org.axonframework.common.configuration.Component.Identifier<>(Integer.class, "cmp");

        assertFalse(first.matches(second));
    }

    @Test
    void equalsAndHashCodeConsistency() {
        org.axonframework.common.configuration.Component.Identifier<String> first =
                new org.axonframework.common.configuration.Component.Identifier<>(String.class, "cmp");
        org.axonframework.common.configuration.Component.Identifier<String> second =
                new org.axonframework.common.configuration.Component.Identifier<>(String.class, "cmp");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertTrue(first.equals(second));
        assertTrue(second.equals(first));
    }

    @Test
    void matchesByTypeRefWithIncompatibleTypesAndDifferentNamesReturnsFalse() {
        // Test the branch where types are incompatible (both type.isAssignableFrom false)
        TypeReference<String> stringType = new TypeReference<String>() {};
        TypeReference<Integer> integerType = new TypeReference<Integer>() {};
        org.axonframework.common.configuration.Component.Identifier<String> first =
                new org.axonframework.common.configuration.Component.Identifier<>(stringType, "first");
        org.axonframework.common.configuration.Component.Identifier<Integer> second =
                new org.axonframework.common.configuration.Component.Identifier<>(integerType, "second");

        assertFalse(first.matchesByTypeRef(second));
        assertFalse(second.matchesByTypeRef(first));
    }

    @Test
    void matchesByTypeRefWithIncompatibleTypesAndSameNamesReturnsFalse() {
        // Test the branch where matchesTypeByTypeRef is false AND names match (both false, short-circuits)
        TypeReference<String> stringType = new TypeReference<String>() {};
        TypeReference<Integer> integerType = new TypeReference<Integer>() {};
        org.axonframework.common.configuration.Component.Identifier<String> first =
                new org.axonframework.common.configuration.Component.Identifier<>(stringType, "same");
        org.axonframework.common.configuration.Component.Identifier<Integer> second =
                new org.axonframework.common.configuration.Component.Identifier<>(integerType, "same");

        assertFalse(first.matchesByTypeRef(second));
        assertFalse(second.matchesByTypeRef(first));
    }

    @Test
    void matchesByTypeRefWithNullNameAndDifferentNamesReturnsFalse() {
        // Test the branch where type matches but names differ (including null vs non-null)
        TypeReference<String> typeRef = new TypeReference<String>() {};
        org.axonframework.common.configuration.Component.Identifier<String> first =
                new org.axonframework.common.configuration.Component.Identifier<>(typeRef, null);
        org.axonframework.common.configuration.Component.Identifier<String> second =
                new org.axonframework.common.configuration.Component.Identifier<>(typeRef, "different");

        assertFalse(first.matchesByTypeRef(second));
        assertFalse(second.matchesByTypeRef(first));
    }

    @Test
    void typeAsClassUnwrapsTypeReference() {
        TypeReference<java.util.Map<String, Integer>> typeRef =
                new TypeReference<java.util.Map<String, Integer>>() {};
        org.axonframework.common.configuration.Component.Identifier<java.util.Map<String, Integer>> identifier =
                new org.axonframework.common.configuration.Component.Identifier<>(typeRef, "map-cmp");

        assertEquals(java.util.Map.class, identifier.typeAsClass());
    }
}
