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

package org.axonframework.common.configuration;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Test class for {@link LifecycleRegistry} interface to achieve 100% coverage.
 */
class LifecycleRegistryTest {

    /**
     * Concrete implementation that records the parameters passed to the abstract methods.
     */
    private static class RecordingRegistry implements LifecycleRegistry {

        long recordedTimeout;
        TimeUnit recordedTimeoutUnit;
        Integer recordedStartPhase;
        LifecycleHandler recordedStartHandler;
        Integer recordedShutdownPhase;
        LifecycleHandler recordedShutdownHandler;

        @Override
        public LifecycleRegistry registerLifecyclePhaseTimeout(long timeout, TimeUnit timeUnit) {
            this.recordedTimeout = timeout;
            this.recordedTimeoutUnit = timeUnit;
            return this;
        }

        @Override
        public LifecycleRegistry onStart(int phase, LifecycleHandler startHandler) {
            this.recordedStartPhase = phase;
            this.recordedStartHandler = startHandler;
            return this;
        }

        @Override
        public LifecycleRegistry onShutdown(int phase, LifecycleHandler shutdownHandler) {
            this.recordedShutdownPhase = phase;
            this.recordedShutdownHandler = shutdownHandler;
            return this;
        }
    }

    @Test
    void registerLifecyclePhaseTimeoutRecordsPhaseAndTimeUnit() {
        RecordingRegistry testSubject = new RecordingRegistry();
        long expectedTimeout = 5;

        testSubject.registerLifecyclePhaseTimeout(expectedTimeout, TimeUnit.SECONDS);

        assertSame(expectedTimeout, testSubject.recordedTimeout);
        assertSame(TimeUnit.SECONDS, testSubject.recordedTimeoutUnit);
    }

    @Test
    void onStartRecordsPhaseAndHandler() {
        RecordingRegistry testSubject = new RecordingRegistry();
        LifecycleHandler handler = configuration -> null;

        testSubject.onStart(3, handler);

        assertSame(3, testSubject.recordedStartPhase);
        assertSame(handler, testSubject.recordedStartHandler);
    }

    @Test
    void onShutdownRecordsPhaseAndHandler() {
        RecordingRegistry testSubject = new RecordingRegistry();
        LifecycleHandler handler = configuration -> null;

        testSubject.onShutdown(7, handler);

        assertSame(7, testSubject.recordedShutdownPhase);
        assertSame(handler, testSubject.recordedShutdownHandler);
    }
}