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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Test class for the {@link LockFactory} functional interface to achieve 100% coverage.
 */
class LockFactoryTest {

    @Test
    void obtainLockReturnsLockForNonNullIdentifier() {
        LockFactory lockFactory = identifier -> new Lock() {
            @Override
            public void release() {}

            @Override
            public boolean isHeld() {
                return true;
            }
        };

        Lock result = lockFactory.obtainLock("resource-1");

        assertNotNull(result);
        assertSame(true, result.isHeld());
    }

    @Test
    void obtainLockCanBeUsedAsLambda() {
        AtomicInteger callCount = new AtomicInteger(0);
        LockFactory lockFactory = identifier -> {
            callCount.incrementAndGet();
            return new Lock() {
                @Override
                public void release() {}

                @Override
                public boolean isHeld() {
                    return callCount.get() > 0;
                }
            };
        };

        Lock firstLock = lockFactory.obtainLock("first");
        Lock secondLock = lockFactory.obtainLock("second");

        assertNotNull(firstLock);
        assertNotNull(secondLock);
        assertEquals(2, callCount.get());
    }

    @Test
    void obtainLockHandlesNullIdentifierDependingOnImplementation() {
        LockFactory nullSafeFactory = identifier -> NoOpLock.INSTANCE;

        // NullLockFactory accepts null identifiers
        Lock lock = nullSafeFactory.obtainLock(null);

        assertNotNull(lock);
        assertSame(NoOpLock.INSTANCE, lock);
    }
}