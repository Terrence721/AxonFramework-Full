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

package org.axonframework.common.lock;

import org.axonframework.common.AxonTransientException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Test class for {@link LockAcquisitionFailedException} to achieve 100% coverage.
 */
class LockAcquisitionFailedExceptionTest {

    @Test
    void constructorWithMessageAndCause() {
        RuntimeException cause = new RuntimeException("lock unavailable");
        LockAcquisitionFailedException testSubject = new LockAcquisitionFailedException("failed to acquire lock", cause);

        assertEquals("failed to acquire lock", testSubject.getMessage());
        assertSame(cause, testSubject.getCause());
    }

    @Test
    void constructorWithMessageOnly() {
        LockAcquisitionFailedException testSubject = new LockAcquisitionFailedException("timeout waiting for lock");

        assertEquals("timeout waiting for lock", testSubject.getMessage());
        assertNull(testSubject.getCause());
    }

    @Test
    void constructorWithNullMessageAndCause() {
        RuntimeException cause = new RuntimeException("underlying issue");
        LockAcquisitionFailedException testSubject = new LockAcquisitionFailedException(null, cause);

        assertNull(testSubject.getMessage());
        assertSame(cause, testSubject.getCause());
    }

    @Test
    void extendsAxonTransientException() {
        assertInstanceOf(AxonTransientException.class, new LockAcquisitionFailedException("test"));
    }

    @Test
    void isEmptyCauseDoesNotAffectInheritance() {
        LockAcquisitionFailedException testSubject = new LockAcquisitionFailedException("no cause");

        assertEquals("no cause", testSubject.getMessage());
        assertNull(testSubject.getCause());
        assertInstanceOf(AxonTransientException.class, testSubject);
    }
}