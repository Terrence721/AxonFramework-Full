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

class AxonExceptionTest {

    private static final class ExampleException extends AxonException {
        ExampleException(String message) {
            super(message);
        }

        ExampleException(String message, Throwable cause) {
            super(message, cause);
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
}
