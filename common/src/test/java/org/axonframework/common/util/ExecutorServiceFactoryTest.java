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

package org.axonframework.common.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Test class for {@link ExecutorServiceFactory} interface to achieve 100% coverage.
 */
class ExecutorServiceFactoryTest {

    @Test
    void createExecutorServiceReturnsExecutorFromImplementation() {
        StubExecutorServiceFactory testSubject = new StubExecutorServiceFactory();
        ExecutorService expected = testSubject.createExpectedService();
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();

        ExecutorService actual = testSubject.createExecutorService("config", queue);

        assertNotNull(actual);
        assertSame(expected, actual);
    }

    private static class StubExecutorServiceFactory implements ExecutorServiceFactory<String> {

        private final ExecutorService executorService = new ThreadPoolExecutor(1, 1, 0L, java.util.concurrent.TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

        @Override
        public ExecutorService createExecutorService(String configuration, BlockingQueue<Runnable> queue) {
            return executorService;
        }

        private ExecutorService createExpectedService() {
            return executorService;
        }
    }
}
