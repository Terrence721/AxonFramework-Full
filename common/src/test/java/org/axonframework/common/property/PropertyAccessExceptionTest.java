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

package org.axonframework.common.property;

import org.axonframework.common.AxonConfigurationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Test class for the {@link PropertyAccessException} class to achieve 100% coverage.
 */
class PropertyAccessExceptionTest {

    @Test
    void constructorWithMessageAndCauseSetsCorrectValues() {
        String message = "Property not accessible";
        Throwable cause = new IllegalArgumentException("Invalid property");

        PropertyAccessException exception = new PropertyAccessException(message, cause);

        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void constructorWithMessageAndNullCauseSetsCorrectValues() {
        String message = "Property not accessible";

        PropertyAccessException exception = new PropertyAccessException(message, null);

        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void constructorWithNullMessageAndCauseHandlesGracefully() {
        Throwable cause = new IllegalArgumentException("Invalid property");

        PropertyAccessException exception = new PropertyAccessException(null, cause);

        assertNotNull(exception);
        assertNull(exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void constructorWithNullMessageAndNullCauseHandlesGracefully() {
        PropertyAccessException exception = new PropertyAccessException(null, null);

        assertNotNull(exception);
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void exceptionExtendsAxonConfigurationException() {
        PropertyAccessException exception = new PropertyAccessException("test", null);

        assertNotNull(exception);
        // Verify it's an instance of the parent class
        assertSame(AxonConfigurationException.class, exception.getClass().getSuperclass());
    }
}