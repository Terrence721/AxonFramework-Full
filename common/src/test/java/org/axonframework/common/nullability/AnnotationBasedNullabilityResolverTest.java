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

package org.axonframework.common.nullability;

import org.junit.jupiter.api.Test;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link AnnotationBasedNullabilityResolver} and the {@link Nullability} values, including
 * the {@link NullabilityResolver} convenience methods.
 */
class AnnotationBasedNullabilityResolverTest {

    private final AnnotationBasedNullabilityResolver testSubject = new AnnotationBasedNullabilityResolver();

    /**
     * Holder for a parameter-level (declaration) annotation named {@code Nullable}, as used by e.g. the Jakarta
     * annotations, in contrast to the type-use annotation of JSpecify.
     */
    static final class Declaration {

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.PARAMETER)
        @interface Nullable {

        }
    }

    /**
     * Same simple name as {@link Declaration.Nullable} apart from its casing, living in its own holder so the class
     * files do not clash on case-insensitive file systems.
     */
    static final class UpperCase {

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.PARAMETER)
        @interface NULLABLE {

        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @interface Unrelated {

    }

    @SuppressWarnings({"unused", "java:S119"})
    static void typeUseNullable(@Nullable String value) {
    }

    @SuppressWarnings({"unused", "java:S119"})
    static void declarationNullable(@Declaration.Nullable String value) {
    }

    @SuppressWarnings({"unused", "java:S119"})
    static void upperCaseNullable(@UpperCase.NULLABLE String value) {
    }

    @SuppressWarnings({"unused", "java:S119"})
    static void nonNullAnnotated(@NonNull String value) {
    }

    @SuppressWarnings({"unused", "java:S119"})
    static void unrelatedAnnotation(@Unrelated String value) {
    }

    @SuppressWarnings({"unused", "java:S119"})
    static void unannotated(String value) {
    }

    private static Parameter parameterOf(String methodName) {
        for (Method method : AnnotationBasedNullabilityResolverTest.class.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method.getParameters()[0];
            }
        }
        throw new IllegalArgumentException("No such method: " + methodName);
    }

    @Test
    void typeUseNullableAnnotationResolvesToNullable() {
        assertEquals(Nullability.NULLABLE, testSubject.resolve(parameterOf("typeUseNullable")));
    }

    @Test
    void parameterLevelNullableAnnotationResolvesToNullable() {
        assertEquals(Nullability.NULLABLE, testSubject.resolve(parameterOf("declarationNullable")));
    }

    @Test
    void annotationNameIsMatchedIgnoringCase() {
        assertEquals(Nullability.NULLABLE, testSubject.resolve(parameterOf("upperCaseNullable")));
    }

    @Test
    void nonNullAnnotationDoesNotResolveToNonNull() {
        assertEquals(Nullability.UNKNOWN, testSubject.resolve(parameterOf("nonNullAnnotated")));
    }

    @Test
    void unrelatedAnnotationResolvesToUnknown() {
        assertEquals(Nullability.UNKNOWN, testSubject.resolve(parameterOf("unrelatedAnnotation")));
    }

    @Test
    void unannotatedParameterResolvesToUnknown() {
        assertEquals(Nullability.UNKNOWN, testSubject.resolve(parameterOf("unannotated")));
    }

    @Test
    void conveniencesDelegateToTheResolverChain() {
        assertTrue(NullabilityResolver.isNullable(parameterOf("typeUseNullable")));
        assertTrue(NullabilityResolver.isNullable(parameterOf("declarationNullable")));
        assertFalse(NullabilityResolver.isNullable(parameterOf("unannotated")));
        assertEquals(Nullability.NULLABLE, NullabilityResolver.nullabilityOf(parameterOf("typeUseNullable")));
        assertEquals(Nullability.UNKNOWN, NullabilityResolver.nullabilityOf(parameterOf("unannotated")));
    }

    @Test
    void nullabilityHasTheThreeDocumentedValues() {
        assertEquals(
                Arrays.asList(Nullability.NULLABLE, Nullability.NON_NULL, Nullability.UNKNOWN),
                Arrays.asList(Nullability.values())
        );
        assertEquals(Nullability.NON_NULL, Nullability.valueOf("NON_NULL"));
    }
}
