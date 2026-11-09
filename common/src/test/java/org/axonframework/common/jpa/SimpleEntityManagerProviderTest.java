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

package org.axonframework.common.jpa;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test class validating the {@link SimpleEntityManagerProvider}.
 */
class SimpleEntityManagerProviderTest {

    @Test
    void constructorRejectsNullEntityManager() {
        NullPointerException exception =
                assertThrows(NullPointerException.class, () -> new SimpleEntityManagerProvider(null));

        assertEquals("entityManager", exception.getMessage());
    }

    @Test
    void getEntityManagerAlwaysReturnsTheInstanceGivenAtConstruction() {
        EntityManager entityManager = (EntityManager) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{EntityManager.class},
                (proxy, method, args) -> null
        );
        SimpleEntityManagerProvider testSubject = new SimpleEntityManagerProvider(entityManager);

        assertSame(entityManager, testSubject.getEntityManager());
        assertSame(entityManager, testSubject.getEntityManager());
    }
}
