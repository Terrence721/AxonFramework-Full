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
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test class validating the {@link FactoryBasedEntityManagerProvider}.
 */
class FactoryBasedEntityManagerProviderTest {

    private static EntityManager newEntityManager() {
        return (EntityManager) Proxy.newProxyInstance(
                FactoryBasedEntityManagerProviderTest.class.getClassLoader(),
                new Class<?>[]{EntityManager.class},
                (proxy, method, args) -> null
        );
    }

    private static EntityManagerFactory factory(AtomicInteger created) {
        return (EntityManagerFactory) Proxy.newProxyInstance(
                FactoryBasedEntityManagerProviderTest.class.getClassLoader(),
                new Class<?>[]{EntityManagerFactory.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("createEntityManager") && method.getParameterCount() == 0) {
                        created.incrementAndGet();
                        return newEntityManager();
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    @Test
    void constructorRejectsNullEntityManagerFactory() {
        NullPointerException exception =
                assertThrows(NullPointerException.class, () -> new FactoryBasedEntityManagerProvider(null));

        assertEquals("entityManagerFactory", exception.getMessage());
    }

    @Test
    void getEntityManagerCreatesNewEntityManagerOnEveryInvocation() {
        AtomicInteger created = new AtomicInteger();
        FactoryBasedEntityManagerProvider testSubject = new FactoryBasedEntityManagerProvider(factory(created));

        EntityManager first = testSubject.getEntityManager();
        EntityManager second = testSubject.getEntityManager();

        assertNotSame(first, second);
        assertEquals(2, created.get());
    }
}
