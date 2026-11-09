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

package org.axonframework.common.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link ClasspathResolver} and the {@link ExecutorServiceFactory} contract.
 */
class ClasspathResolverTest {

    @Test
    void projectReactorIsDetectedOnTheTestClasspath() {
        assertTrue(ClasspathResolver.projectReactorOnClasspath());
    }

    @Test
    void detectionMatchesWhetherFluxCanBeLoaded() {
        boolean loadable;
        try {
            Class.forName("reactor.core.publisher.Flux", false, ClasspathResolver.class.getClassLoader());
            loadable = true;
        } catch (ClassNotFoundException e) {
            loadable = false;
        }

        assertEquals(loadable, ClasspathResolver.projectReactorOnClasspath());
    }

    @Test
    void isFinalNonInstantiableUtilityClass() {
        Constructor<?>[] constructors = ClasspathResolver.class.getDeclaredConstructors();

        assertTrue(Modifier.isFinal(ClasspathResolver.class.getModifiers()));
        assertEquals(1, constructors.length);
        assertTrue(Modifier.isPrivate(constructors[0].getModifiers()));
    }

    @Test
    void executorServiceFactoryReceivesConfigurationAndQueue() {
        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
        ExecutorServiceFactory<Integer> testSubject = (threads, workQueue) -> {
            assertEquals(queue, workQueue);
            return Executors.newFixedThreadPool(threads);
        };

        ExecutorService result = testSubject.createExecutorService(2, queue);

        assertNotNull(result);
        result.shutdownNow();
    }
}
