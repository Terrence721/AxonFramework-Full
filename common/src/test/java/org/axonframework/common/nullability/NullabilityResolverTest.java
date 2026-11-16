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

package org.axonframework.common.nullability;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for the {@link NullabilityResolver} interface to achieve 100% coverage.
 */
class NullabilityResolverTest {

    // Test class with various parameter annotations
    public static class TestClass {
        public void nullableParam(@javax.annotation.Nullable String value) {
        }

        public void nonNullParam(@javax.annotation.Nonnull String value) {
        }

        public void plainParam(String value) {
        }
    }

    @Test
    void nullabilityOfDelegatesToResolverChain() throws NoSuchMethodException {
        Method method = TestClass.class.getMethod("nullableParam", String.class);
        Parameter parameter = method.getParameters()[0];

        Nullability result = NullabilityResolver.nullabilityOf(parameter);

        assertNotNull(result);
    }

    @Test
    void isNullableReturnsTrueForNullableParameter() throws NoSuchMethodException {
        Method method = TestClass.class.getMethod("nullableParam", String.class);
        Parameter parameter = method.getParameters()[0];

        boolean result = NullabilityResolver.isNullable(parameter);

        assertTrue(result);
    }

    @Test
    void isNullableReturnsFalseForNonNullParameter() throws NoSuchMethodException {
        Method method = TestClass.class.getMethod("nonNullParam", String.class);
        Parameter parameter = method.getParameters()[0];

        boolean result = NullabilityResolver.isNullable(parameter);

        assertFalse(result);
    }

    @Test
    void isNullableReturnsFalseForPlainParameter() throws NoSuchMethodException {
        Method method = TestClass.class.getMethod("plainParam", String.class);
        Parameter parameter = method.getParameters()[0];

        boolean result = NullabilityResolver.isNullable(parameter);

        assertFalse(result);
    }

    @Test
    void nullabilityOfReturnsUnknownForPlainParameter() throws NoSuchMethodException {
        Method method = TestClass.class.getMethod("plainParam", String.class);
        Parameter parameter = method.getParameters()[0];

        Nullability result = NullabilityResolver.nullabilityOf(parameter);

        assertEquals(Nullability.UNKNOWN, result);
    }

    @Test
    void nullabilityOfReturnsNullableForNullableParameter() throws NoSuchMethodException {
        Method method = TestClass.class.getMethod("nullableParam", String.class);
        Parameter parameter = method.getParameters()[0];

        Nullability result = NullabilityResolver.nullabilityOf(parameter);

        assertEquals(Nullability.NULLABLE, result);
    }

    @Test
    void nullabilityOfReturnsNonNullForNonNullParameter() throws NoSuchMethodException {
        Method method = TestClass.class.getMethod("nonNullParam", String.class);
        Parameter parameter = method.getParameters()[0];

        Nullability result = NullabilityResolver.nullabilityOf(parameter);

        assertEquals(Nullability.NON_NULL, result);
    }
}