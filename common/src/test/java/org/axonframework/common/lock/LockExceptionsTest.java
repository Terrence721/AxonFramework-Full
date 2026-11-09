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

package org.axonframework.common.lock;

import org.axonframework.common.AxonTransientException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Test class validating the {@link LockAcquisitionFailedException} and {@link DeadlockException}.
 */
class LockExceptionsTest {

    @Test
    void lockAcquisitionFailedExceptionRetainsMessageAndCause() {
        InterruptedException cause = new InterruptedException("interrupted");

        LockAcquisitionFailedException testSubject = new LockAcquisitionFailedException("failed", cause);

        assertEquals("failed", testSubject.getMessage());
        assertSame(cause, testSubject.getCause());
    }

    @Test
    void lockAcquisitionFailedExceptionWithoutCause() {
        LockAcquisitionFailedException testSubject = new LockAcquisitionFailedException("failed");

        assertEquals("failed", testSubject.getMessage());
        assertNull(testSubject.getCause());
    }

    @Test
    void lockAcquisitionFailedExceptionIsTransient() {
        assertInstanceOf(AxonTransientException.class, new LockAcquisitionFailedException("failed"));
    }

    @Test
    void deadlockExceptionIsALockAcquisitionFailure() {
        DeadlockException testSubject = new DeadlockException("deadlock");

        assertEquals("deadlock", testSubject.getMessage());
        assertInstanceOf(LockAcquisitionFailedException.class, testSubject);
    }
}
