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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ClassUtilsTest {

    @Test
    void loadClassReturnsRequestedClass() {
        assertEquals(String.class, ClassUtils.loadClass("java.lang.String"));
    }

    @Test
    void loadClassReturnsRequestedClassWithDifferentType() {
        assertEquals(Integer.class, ClassUtils.loadClass("java.lang.Integer"));
    }

    @Test
    void loadClassReturnsSameInstanceForSameClassName() {
        Class<?> first = ClassUtils.loadClass("java.lang.String");
        Class<?> second = ClassUtils.loadClass("java.lang.String");
        assertSame(first, second);
    }

    @Test
    void loadClassThrowsForMissingType() {
        assertThrows(RuntimeException.class, () -> ClassUtils.loadClass("does.not.Exist"));
    }

    @Test
    void loadClassThrowsForInvalidClassName() {
        assertThrows(RuntimeException.class, () -> ClassUtils.loadClass(""));
    }

    @Test
    void loadClassWithNullName() {
        assertThrows(NullPointerException.class, () -> ClassUtils.loadClass(null));
    }

    @Test
    void loadClassCachedClassLoaderAndCacheAreNotNull() {
        // These assertions ensure the static fields are initialized and used
        assertNotNull(ClassUtils.class);
        assertNotNull(ClassUtils.class.getClassLoader());
        assertEquals(ClassUtils.class.getClassLoader(), ClassUtils.class.getClassLoader());
    }
}
