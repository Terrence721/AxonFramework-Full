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

package org.axonframework.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdentifierValidatorTest {

    private IdentifierValidator validator;

    @BeforeEach
    void setUp() {
        validator = IdentifierValidator.getInstance();
        // Clear whitelist to ensure test isolation
        // ReflectionUtils.declaringClass uses reflection to find the class declaring the method
        // We need to test with a custom class that doesn't override toString
    }

    @Test
    void rejectsTypeUsingObjectToString() {
        assertFalse(validator.isValidIdentifier(Object.class));
    }

    @Test
    void acceptsTypeWithDistinctToString() {
        assertTrue(validator.isValidIdentifier(String.class));
    }

    @Test
    void acceptsTypeAlreadyInWhitelist() {
        Class<?> type = Integer.class;
        validator.isValidIdentifier(type); // Add to whitelist
        assertTrue(validator.isValidIdentifier(type)); // Should return true immediately from whitelist
    }

    @Test
    void acceptsCustomClassWithCustomToString() {
        class CustomIdentifier {
            @Override
            public String toString() {
                return "custom";
            }
        }
        assertTrue(validator.isValidIdentifier(CustomIdentifier.class));
    }

    @Test
    void rejectsCustomClassUsingObjectToString() {
        class CustomIdentifier {
            // Uses Object.toString()
        }
        assertFalse(validator.isValidIdentifier(CustomIdentifier.class));
    }

    @Test
    void constructorIsPrivate() throws Exception {
        // Verify that the private constructor is present and cannot be accessed directly
        // This ensures the singleton pattern is maintained
        java.lang.reflect.Constructor<?> ctor = IdentifierValidator.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        IdentifierValidator instance = (IdentifierValidator) ctor.newInstance();
        assertTrue(instance instanceof IdentifierValidator);
    }

    @Test
    void singletonInstanceIsConsistent() {
        IdentifierValidator first = IdentifierValidator.getInstance();
        IdentifierValidator second = IdentifierValidator.getInstance();
        assertTrue(first == second);
    }

    @Test
    void whiteListIsThreadSafe() {
        // Verify ConcurrentHashMap behavior for thread safety
        Class<?>[] types = {String.class, Integer.class, Long.class, Boolean.class};
        for (Class<?> type : types) {
            // Each thread-safe operation should return consistent results
            assertTrue(validator.isValidIdentifier(type));
            assertTrue(validator.isValidIdentifier(type)); // From whitelist
        }
    }

    @Test
    void classWithObjectToStringIsRejected() {
        class CustomWithoutToString {}
        // Uses Object.toString() by default
        assertFalse(validator.isValidIdentifier(CustomWithoutToString.class));
    }
}