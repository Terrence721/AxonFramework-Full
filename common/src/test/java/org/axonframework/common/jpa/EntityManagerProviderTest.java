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

package org.axonframework.common.jpa;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Test class for {@link EntityManagerProvider} interface to achieve 100% coverage.
 */
class EntityManagerProviderTest {

    private static EntityManager stubEntityManager() {
        return (EntityManager) Proxy.newProxyInstance(
                EntityManagerProviderTest.class.getClassLoader(),
                new Class<?>[]{EntityManager.class},
                (proxy, method, args) -> null
        );
    }

    @Test
    void getEntityManagerReturnsEntityFromImplementation() {
        StubEntityManagerProvider testSubject = new StubEntityManagerProvider();
        EntityManager expectedManager = testSubject.createEntityManager();

        EntityManager actualManager = testSubject.getEntityManager();

        assertNotNull(actualManager);
        assertSame(expectedManager, actualManager);
    }

    private static class StubEntityManagerProvider implements EntityManagerProvider {

        private final EntityManager entityManager = stubEntityManager();

        @Override
        public EntityManager getEntityManager() {
            return entityManager;
        }

        private EntityManager createEntityManager() {
            return entityManager;
        }
    }
}
