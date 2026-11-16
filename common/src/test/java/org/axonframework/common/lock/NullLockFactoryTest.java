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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for the {@link NullLockFactory} enum to achieve 100% coverage.
 */
class NullLockFactoryTest {

    @Test
    void instanceIsSingleton() {
        assertSame(NullLockFactory.INSTANCE, NullLockFactory.INSTANCE);
        assertSame(NullLockFactory.INSTANCE, NullLockFactory.valueOf("INSTANCE"));
        assertEquals(1, NullLockFactory.values().length);
    }

    @Test
    void obtainLockReturnsNoOpLockForNonNullIdentifier() {
        Lock result = NullLockFactory.INSTANCE.obtainLock("resource-1");

        assertNotNull(result);
        assertSame(NoOpLock.INSTANCE, result);
        assertTrue(result.isHeld());
    }

    @Test
    void obtainLockReturnsNoOpLockForNullIdentifier() {
        Lock result = NullLockFactory.INSTANCE.obtainLock(null);

        assertNotNull(result);
        assertSame(NoOpLock.INSTANCE, result);
        assertTrue(result.isHeld());
    }

    @Test
    void obtainLockReturnsNoOpLockForEmptyIdentifier() {
        Lock result = NullLockFactory.INSTANCE.obtainLock("");

        assertNotNull(result);
        assertSame(NoOpLock.INSTANCE, result);
        assertTrue(result.isHeld());
    }

    @Test
    void obtainedLockCanBeReleased() {
        Lock lock = NullLockFactory.INSTANCE.obtainLock("test");

        assertTrue(lock.isHeld());
        lock.release();
        assertTrue(lock.isHeld()); // NoOpLock always returns true
        lock.close();
    }
}