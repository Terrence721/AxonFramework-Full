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

package org.axonframework.common.lifecycle;

import org.axonframework.common.AxonNonTransientException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for the {@link ShutdownInProgressException} class to achieve 100% coverage.
 */
class ShutdownInProgressExceptionTest {

    @Test
    void defaultConstructorSetsDefaultMessage() {
        ShutdownInProgressException exception = new ShutdownInProgressException();

        assertNotNull(exception);
        assertEquals("Cannot start the activity, shutdown in progress", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void constructorWithMessageSetsCustomMessage() {
        String customMessage = "Custom shutdown message";

        ShutdownInProgressException exception = new ShutdownInProgressException(customMessage);

        assertNotNull(exception);
        assertEquals(customMessage, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void constructorWithNullMessageHandlesGracefully() {
        ShutdownInProgressException exception = new ShutdownInProgressException(null);

        assertNotNull(exception);
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void defaultMessageIsCorrect() {
        ShutdownInProgressException exception = new ShutdownInProgressException();
        assertEquals("Cannot start the activity, shutdown in progress", exception.getMessage());
    }

    
    @Test
    void exceptionExtendsAxonNonTransientException() {
        ShutdownInProgressException exception = new ShutdownInProgressException("test");

        assertNotNull(exception);
        assertTrue(exception instanceof AxonNonTransientException);
        assertSame(AxonNonTransientException.class, exception.getClass().getSuperclass());
    }
}