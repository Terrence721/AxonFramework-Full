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

package org.axonframework.common.lifecycle;

import org.axonframework.common.AxonNonTransientException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Test class validating the {@link LifecycleHandlerInvocationException} and {@link ShutdownInProgressException}.
 */
class LifecycleExceptionsTest {

    @Test
    void lifecycleHandlerInvocationExceptionRetainsMessageAndCause() {
        IllegalStateException cause = new IllegalStateException("handler failed");

        LifecycleHandlerInvocationException testSubject = new LifecycleHandlerInvocationException("start failed", cause);

        assertEquals("start failed", testSubject.getMessage());
        assertSame(cause, testSubject.getCause());
    }

    @Test
    void lifecycleHandlerInvocationExceptionAcceptsNullCause() {
        LifecycleHandlerInvocationException testSubject = new LifecycleHandlerInvocationException("start failed", null);

        assertNull(testSubject.getCause());
    }

    @Test
    void shutdownInProgressExceptionUsesDefaultMessage() {
        ShutdownInProgressException testSubject = new ShutdownInProgressException();

        assertEquals("Cannot start the activity, shutdown in progress", testSubject.getMessage());
    }

    @Test
    void shutdownInProgressExceptionUsesGivenMessage() {
        ShutdownInProgressException testSubject = new ShutdownInProgressException("custom");

        assertEquals("custom", testSubject.getMessage());
    }

    @Test
    void shutdownInProgressExceptionIsNonTransient() {
        assertInstanceOf(AxonNonTransientException.class, new ShutdownInProgressException());
    }
}
