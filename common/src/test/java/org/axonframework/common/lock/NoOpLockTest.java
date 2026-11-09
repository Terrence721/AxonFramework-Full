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

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class validating the {@link NoOpLock}, the {@link NullLockFactory} and the default behavior of {@link Lock}.
 */
class NoOpLockTest {

    @Test
    void noOpLockIsAlwaysHeldAndNeverReleased() {
        Lock testSubject = NoOpLock.INSTANCE;

        assertTrue(testSubject.isHeld());
        assertDoesNotThrow(testSubject::release);
        assertTrue(testSubject.isHeld());
    }

    @Test
    void noOpLockCanOnlyBeObtainedThroughItsSingleton() {
        Constructor<?>[] constructors = NoOpLock.class.getDeclaredConstructors();

        assertEquals(1, constructors.length);
        assertTrue(Modifier.isPrivate(constructors[0].getModifiers()));
    }

    @Test
    void nullLockFactoryAlwaysReturnsTheNoOpLock() {
        assertSame(NoOpLock.INSTANCE, NullLockFactory.INSTANCE.obtainLock("first"));
        assertSame(NoOpLock.INSTANCE, NullLockFactory.INSTANCE.obtainLock("second"));
    }

    @Test
    void nullLockFactoryAcceptsNullIdentifier() {
        assertSame(NoOpLock.INSTANCE, NullLockFactory.INSTANCE.obtainLock(null));
    }

    @Test
    void lockFactoryIsUsableAsLambda() {
        LockFactory testSubject = identifier -> NoOpLock.INSTANCE;

        assertSame(NoOpLock.INSTANCE, testSubject.obtainLock("anything"));
    }

    @Test
    void defaultCloseDelegatesToRelease() {
        AtomicInteger releases = new AtomicInteger();
        Lock testSubject = new Lock() {
            @Override
            public void release() {
                releases.incrementAndGet();
            }

            @Override
            public boolean isHeld() {
                return releases.get() == 0;
            }
        };

        testSubject.close();

        assertEquals(1, releases.get());
    }
}
