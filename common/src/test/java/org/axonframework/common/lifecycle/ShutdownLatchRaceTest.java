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

package org.axonframework.common.lifecycle;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Stress test for the concurrent use of the {@link ShutdownLatch}.
 */
class ShutdownLatchRaceTest {

    /**
     * Races {@link ShutdownLatch#registerActivity()} against {@link ShutdownLatch#initiateShutdown()}. Whichever wins,
     * the shutdown future must complete once no activity remains: either the registration was rejected (so there is
     * nothing to wait for) or it succeeded and ended.
     */
    @Test
    void shutdownCompletesWhenRegistrationRacesWithInitiateShutdown() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            for (int i = 0; i < 20_000; i++) {
                ShutdownLatch latch = new ShutdownLatch();
                CyclicBarrier barrier = new CyclicBarrier(2);
                AtomicBoolean registered = new AtomicBoolean();

                Future<?> registrar = executor.submit(() -> {
                    barrier.await();
                    try {
                        latch.registerActivity().end();
                        registered.set(true);
                    } catch (ShutdownInProgressException expected) {
                        // Rejected: shutdown won the race.
                    }
                    return null;
                });
                Future<CompletableFuture<Void>> initiator = executor.submit(() -> {
                    barrier.await();
                    return latch.initiateShutdown();
                });

                registrar.get(5, TimeUnit.SECONDS);
                CompletableFuture<Void> shutdown = initiator.get(5, TimeUnit.SECONDS);

                assertTrue(shutdown.isDone(),
                           "Shutdown future never completed on iteration " + i + " (registered=" + registered + ")");
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
