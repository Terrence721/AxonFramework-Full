/*
 * Copyright (c) 2010-2026. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance乖 compliance with the License.
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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Test class for {@link LifecycleHandlerInvocationException} to achieve 100% coverage.
 */
class LifecycleHandlerInvocationExceptionTest {

    @Test
    void constructorWithMessageAndCause() {
        IllegalStateException cause = new IllegalStateException("handler failed");
        LifecycleHandlerInvocationException testSubject = new LifecycleHandlerInvocationException("start failed", cause);

        assertEquals("start failed", testSubject.getMessage());
        assertSame(cause, testSubject.getCause());
    }

    @Test
    void constructorWithMessageAndNullCause() {
        LifecycleHandlerInvocationException testSubject = new LifecycleHandlerInvocationException("start failed", null);

        assertEquals("start failed", testSubject.getMessage());
        assertNull(testSubject.getCause());
    }

    @Test
    void constructorWithEmptyMessageAndCause() {
        RuntimeException cause = new RuntimeException("error");
        LifecycleHandlerInvocationException testSubject = new LifecycleHandlerInvocationException("", cause);

        assertEquals("", testSubject.getMessage());
        assertSame(cause, testSubject.getCause());
    }

    @Test
    void constructorWithNullMessageAndCause() {
        RuntimeException cause = new RuntimeException();
        LifecycleHandlerInvocationException testSubject = new LifecycleHandlerInvocationException(null, cause);

        assertNull(testSubject.getMessage());
        assertSame(cause, testSubject.getCause());
    }
}