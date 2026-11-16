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

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for the {@link PriorityRunnable} class to achieve 100% coverage.
 */
class PriorityRunnableTest {

    @Test
    void runnableImplementationRunsCorrectly() {
        AtomicBoolean executed = new AtomicBoolean(false);
        Runnable underlyingRunnable = () -> executed.set(true);
        PriorityRunnable priorityRunnable = new PriorityRunnable(underlyingRunnable, 10, 1);

        priorityRunnable.run();
        assertTrue(executed.get());
    }

    @Test
    void priorityAndSequenceSetCorrectly() {
        Runnable underlyingRunnable = () -> {};
        PriorityRunnable priorityRunnable = new PriorityRunnable(underlyingRunnable, 100, 50);

        assertEquals(100, priorityRunnable.priority());
        assertEquals(50, priorityRunnable.sequence());
    }

    @Test
    void runnableComparisonWorks() {
        Runnable runnable1 = () -> {};
        Runnable runnable2 = () -> {};
        PriorityRunnable priorityTask1 = new PriorityRunnable(runnable1, 20, 10);
        PriorityRunnable priorityTask2 = new PriorityRunnable(runnable2, 10, 5);

        // Task with higher priority should be considered "less than" in compareTo
        int comparison = priorityTask1.compareTo(priorityTask2);
        assertTrue(comparison < 0);
    }

    @Test
    void runnableWithSamePriorityUsesSequence() {
        Runnable runnable1 = () -> {};
        Runnable runnable2 = () -> {};
        PriorityRunnable priorityTask1 = new PriorityRunnable(runnable1, 10, 20);
        PriorityRunnable priorityTask2 = new PriorityRunnable(runnable2, 10, 5);

        // Same priority, different sequences
        int comparison = priorityTask1.compareTo(priorityTask2);
        // PriorityTask.compareTo returns Long.compare(this.sequence(), that.sequence()) for same priority
        // With sequence1=20, sequence2=5, Long.compare(20, 5) = 1 (positive)
        // So priorityTask1 > priorityTask2
        assertTrue(comparison > 0);
    }

    @Test
    void runnableEqualsSameTask() {
        Runnable runnable = () -> {};
        PriorityRunnable priorityTask1 = new PriorityRunnable(runnable, 15, 8);
        PriorityRunnable priorityTask2 = new PriorityRunnable(runnable, 15, 8);

        assertEquals(priorityTask1, priorityTask2);
        assertEquals(priorityTask2.hashCode(), priorityTask1.hashCode());
    }

    @Test
    void runnableNotEqualsDifferentPriority() {
        Runnable runnable = () -> {};
        PriorityRunnable priorityTask1 = new PriorityRunnable(runnable, 10, 5);
        PriorityRunnable priorityTask2 = new PriorityRunnable(runnable, 20, 5);

        assertNotEquals(priorityTask1, priorityTask2);
    }

    @Test
    void runnableNotEqualsDifferentSequence() {
        Runnable runnable = () -> {};
        PriorityRunnable priorityTask1 = new PriorityRunnable(runnable, 10, 5);
        PriorityRunnable priorityTask2 = new PriorityRunnable(runnable, 10, 15);

        assertNotEquals(priorityTask1, priorityTask2);
    }

    @Test
    void runnableToStringContainsAllComponents() {
        Runnable runnable = () -> {};
        PriorityRunnable priorityTask = new PriorityRunnable(runnable, 25, 12);

        String string = priorityTask.toString();
        assertTrue(string.contains("PriorityRunnable"));
        assertTrue(string.contains("task="));
        assertTrue(string.contains("priority=25"));
        assertTrue(string.contains("sequence=12"));
    }

    @Test
    void runnableWithZeroPriorityAndSequence() {
        Runnable runnable = () -> {};
        PriorityRunnable priorityTask = new PriorityRunnable(runnable, 0, 0);

        assertEquals(0, priorityTask.priority());
        assertEquals(0, priorityTask.sequence());
    }

    @Test
    void runnableWithNegativePriority() {
        Runnable runnable = () -> {};
        PriorityRunnable priorityTask = new PriorityRunnable(runnable, -10, 5);

        assertEquals(-10, priorityTask.priority());
        assertEquals(5, priorityTask.sequence());
    }

    @Test
    void runnableWithHighSequence() {
        Runnable runnable = () -> {};
        PriorityRunnable priorityTask = new PriorityRunnable(runnable, 5, 999999);

        assertEquals(999999, priorityTask.sequence());
    }

    @Test
    void runnableEqualsWithNullTask() {
        Runnable runnable = () -> {};
        PriorityRunnable priorityTask = new PriorityRunnable(runnable, 5, 5);

        assertNotEquals(priorityTask, null);
    }

    @Test
    void runnableEqualsWithDifferentClass() {
        Runnable runnable = () -> {};
        PriorityRunnable priorityTask = new PriorityRunnable(runnable, 5, 5);

        assertNotEquals(priorityTask, "Not a PriorityRunnable");
    }
}