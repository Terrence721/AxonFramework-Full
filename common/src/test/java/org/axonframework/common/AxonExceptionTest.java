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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AxonExceptionTest {

    private static final class ExampleException extends AxonException {
        ExampleException(String message) {
            super(message);
        }

        ExampleException(String message, Throwable cause) {
            super(message, cause);
        }

        ExampleException(String message, Throwable cause, boolean writableStackTrace) {
            super(message, cause, writableStackTrace);
        }
    }

    @Test
    void messageConstructorPreservesMessage() {
        ExampleException exception = new ExampleException("boom");

        assertEquals("boom", exception.getMessage());
    }

    @Test
    void causeConstructorPreservesCause() {
        RuntimeException cause = new RuntimeException("root");
        ExampleException exception = new ExampleException("boom", cause);

        assertSame(cause, exception.getCause());
    }

    @Test
    void writableStackTraceConstructorWithTrue() {
        RuntimeException cause = new RuntimeException("root");
        ExampleException exception = new ExampleException("boom", cause, true);
        assertEquals("boom", exception.getMessage());
        assertSame(cause, exception.getCause());
        assertNotNull(exception.getStackTrace());
        assertTrue(exception.getStackTrace().length > 0);
    }

    @Test
    void writableStackTraceConstructorWithFalse() {
        RuntimeException cause = new RuntimeException("root");
        // With writableStackTrace=false, getMessage() still works
        ExampleException exception = new ExampleException("boom", cause, false);
        assertEquals("boom", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
