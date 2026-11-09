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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link PriorityTask} ordering together with the {@link PriorityRunnable} and
 * {@link PriorityCallable} implementations built on {@link AbstractPriorityTask}.
 */
class PriorityTaskTest {

    private static final Runnable NOOP = () -> {
    };

    @Test
    void higherPriorityOrdersBeforeLowerPriority() {
        PriorityRunnable high = new PriorityRunnable(NOOP, 10, 5);
        PriorityRunnable low = new PriorityRunnable(NOOP, 1, 1);

        assertTrue(high.compareTo(low) < 0);
        assertTrue(low.compareTo(high) > 0);
    }

    @Test
    void lowerSequenceOrdersFirstWhenPrioritiesAreEqual() {
        PriorityRunnable earlier = new PriorityRunnable(NOOP, 5, 1);
        PriorityRunnable later = new PriorityRunnable(NOOP, 5, 2);

        assertTrue(earlier.compareTo(later) < 0);
        assertTrue(later.compareTo(earlier) > 0);
    }

    @Test
    void compareToIsZeroForSamePriorityAndSequence() {
        assertEquals(0, new PriorityRunnable(NOOP, 5, 1).compareTo(new PriorityRunnable(NOOP, 5, 1)));
    }

    @Test
    void runnablesAndCallablesAreOrderedTogether() {
        PriorityBlockingQueue<PriorityTask> queue = new PriorityBlockingQueue<>();
        PriorityRunnable lowRunnable = new PriorityRunnable(NOOP, 1, 1);
        PriorityCallable<String> highCallable = new PriorityCallable<>(() -> "x", 9, 2);
        PriorityRunnable midRunnableLate = new PriorityRunnable(NOOP, 5, 4);
        PriorityCallable<String> midCallableEarly = new PriorityCallable<>(() -> "y", 5, 3);
        queue.addAll(List.of(lowRunnable, midRunnableLate, highCallable, midCallableEarly));

        List<PriorityTask> drained = new ArrayList<>();
        queue.drainTo(drained);

        assertEquals(List.of(highCallable, midCallableEarly, midRunnableLate, lowRunnable), drained);
    }

    @Test
    void exposesPriorityAndSequence() {
        PriorityRunnable testSubject = new PriorityRunnable(NOOP, 7, 3);

        assertEquals(7, testSubject.priority());
        assertEquals(3, testSubject.sequence());
    }

    @Test
    void priorityRunnableRunsTheDelegate() {
        AtomicInteger runs = new AtomicInteger();

        new PriorityRunnable(runs::incrementAndGet, 1, 1).run();

        assertEquals(1, runs.get());
    }

    @Test
    void priorityCallableReturnsTheDelegateResult() throws Exception {
        assertEquals("result", new PriorityCallable<>(() -> "result", 1, 1).call());
    }

    @Test
    void priorityCallablePropagatesDelegateException() {
        Exception failure = new Exception("boom");
        Callable<String> failing = () -> {
            throw failure;
        };

        Exception thrown = assertThrows(Exception.class, () -> new PriorityCallable<>(failing, 1, 1).call());

        assertSame(failure, thrown);
    }

    @Test
    void equalsAndHashCodeDependOnTaskPriorityAndSequence() {
        PriorityRunnable original = new PriorityRunnable(NOOP, 1, 1);

        assertEquals(original, new PriorityRunnable(NOOP, 1, 1));
        assertEquals(original.hashCode(), new PriorityRunnable(NOOP, 1, 1).hashCode());
        assertEquals(original, original);
        assertNotEquals(original, new PriorityRunnable(NOOP, 2, 1));
        assertNotEquals(original, new PriorityRunnable(NOOP, 1, 2));
        assertNotEquals(original, new PriorityRunnable(() -> {
        }, 1, 1));
        assertNotEquals(original, null);
        assertNotEquals(original, "not a task");
    }

    @Test
    void runnableAndCallableWithSameValuesAreNotEqual() {
        Runnable runnable = NOOP;
        Callable<Object> callable = () -> null;

        assertNotEquals(new PriorityRunnable(runnable, 1, 1), new PriorityCallable<>(callable, 1, 1));
    }

    @Test
    void toStringContainsTypeTaskPriorityAndSequence() {
        PriorityRunnable testSubject = new PriorityRunnable(NOOP, 7, 3);

        String result = testSubject.toString();

        assertTrue(result.startsWith("PriorityRunnable{task="));
        assertTrue(result.endsWith(", priority=7, sequence=3}"));
    }
}
