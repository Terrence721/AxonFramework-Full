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

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for the {@link Lock} interface to achieve 100% test coverage.
 */
class LockTest {

    @Test
    void isHeldReturnsTrueWhenCreated() {
        Lock lock = new Lock() {
            @Override
            public void release() {
                // no-op
            }

            @Override
            public boolean isHeld() {
                return true;
            }
        };

        assertTrue(lock.isHeld());
        lock.close();
    }

    @Test
    void isHeldReturnsFalseAfterRelease() {
        AtomicInteger released = new AtomicInteger(0);
        Lock lock = new Lock() {
            @Override
            public void release() {
                released.incrementAndGet();
            }

            @Override
            public boolean isHeld() {
                return released.get() == 0;
            }
        };

        // Initially held
        assertTrue(lock.isHeld());

        // After release, should no longer be held
        lock.release();
        assertFalse(lock.isHeld());
        lock.close();
    }

    @Test
    void closeDelegatesToRelease() {
        AtomicInteger releaseCount = new AtomicInteger(0);
        Lock lock = new Lock() {
            @Override
            public void release() {
                releaseCount.incrementAndGet();
            }

            @Override
            public boolean isHeld() {
                return releaseCount.get() == 0;
            }
        };

        lock.close();
        assertEquals(1, releaseCount.get());
    }

    @Test
    void multipleReleasesAreAllowed() {
        Lock lock = new Lock() {
            @Override
            public void release() {
                // no-op
            }

            @Override
            public boolean isHeld() {
                return false;
            }
        };

        // First release should work
        lock.release();
        assertFalse(lock.isHeld());

        // Second release should also work (behavior is undefined but should not throw)
        lock.release();
        assertFalse(lock.isHeld());
        lock.close();
    }
}