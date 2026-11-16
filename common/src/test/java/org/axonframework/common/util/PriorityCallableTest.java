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

import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for the {@link PriorityCallable} class to achieve 100% coverage.
 */
class PriorityCallableTest {

    @Test
    void callableImplementationReturnsCorrectResult() throws Exception {
        Callable<String> underlyingCallable = () -> "Hello World";
        PriorityCallable<String> priorityCallable = new PriorityCallable<>(underlyingCallable, 10, 1);

        String result = priorityCallable.call();
        assertEquals("Hello World", result);
    }

    @Test
    void callableThrowsException() {
        Callable<String> underlyingCallable = () -> {
            throw new RuntimeException("Test exception");
        };
        PriorityCallable<String> priorityCallable = new PriorityCallable<>(underlyingCallable, 5, 2);

        Exception exception = assertThrows(Exception.class, () -> priorityCallable.call());
        assertTrue(exception.getMessage().contains("Test exception"));
    }

    @Test
    void priorityAndSequenceSetCorrectly() {
        Callable<String> underlyingCallable = () -> "Test";
        PriorityCallable<String> priorityCallable = new PriorityCallable<>(underlyingCallable, 100, 50);

        assertEquals(100, priorityCallable.priority());
        assertEquals(50, priorityCallable.sequence());
    }

    @Test
    void callableComparisonWorks() {
        Callable<String> callable1 = () -> "Task 1";
        Callable<String> callable2 = () -> "Task 2";
        PriorityCallable<String> priorityTask1 = new PriorityCallable<>(callable1, 20, 10);
        PriorityCallable<String> priorityTask2 = new PriorityCallable<>(callable2, 10, 5);

        // Task with higher priority should be considered "less than" in compareTo
        int comparison = priorityTask1.compareTo(priorityTask2);
        assertTrue(comparison < 0);
    }

    @Test
    void callableWithSamePriorityUsesSequence() {
        Callable<String> callable1 = () -> "Task 1";
        Callable<String> callable2 = () -> "Task 2";
        PriorityCallable<String> priorityTask1 = new PriorityCallable<>(callable1, 10, 20);
        PriorityCallable<String> priorityTask2 = new PriorityCallable<>(callable2, 10, 5);

        // Same priority, different sequences
        int comparison = priorityTask1.compareTo(priorityTask2);
        // Task with lower sequence should be considered "less than" (earlier)
        assertTrue(comparison > 0);
    }

    @Test
    void callableEqualsSameTask() {
        Callable<String> callable = () -> "Test";
        PriorityCallable<String> priorityTask1 = new PriorityCallable<>(callable, 15, 8);
        PriorityCallable<String> priorityTask2 = new PriorityCallable<>(callable, 15, 8);

        assertEquals(priorityTask1, priorityTask2);
        assertEquals(priorityTask2.hashCode(), priorityTask1.hashCode());
    }

    @Test
    void callableNotEqualsDifferentPriority() {
        Callable<String> callable = () -> "Test";
        PriorityCallable<String> priorityTask1 = new PriorityCallable<>(callable, 10, 5);
        PriorityCallable<String> priorityTask2 = new PriorityCallable<>(callable, 20, 5);

        assertNotEquals(priorityTask1, priorityTask2);
    }

    @Test
    void callableNotEqualsDifferentSequence() {
        Callable<String> callable = () -> "Test";
        PriorityCallable<String> priorityTask1 = new PriorityCallable<>(callable, 10, 5);
        PriorityCallable<String> priorityTask2 = new PriorityCallable<>(callable, 10, 15);

        assertNotEquals(priorityTask1, priorityTask2);
    }

    @Test
    void callableToStringContainsAllComponents() {
        Callable<String> callable = () -> "Test";
        PriorityCallable<String> priorityTask = new PriorityCallable<>(callable, 25, 12);

        String string = priorityTask.toString();
        assertTrue(string.contains("PriorityCallable"));
        assertTrue(string.contains("task="));
        assertTrue(string.contains("priority=25"));
        assertTrue(string.contains("sequence=12"));
    }

    @Test
    void callableWithZeroPriorityAndSequence() {
        Callable<String> callable = () -> "Test";
        PriorityCallable<String> priorityTask = new PriorityCallable<>(callable, 0, 0);

        assertEquals(0, priorityTask.priority());
        assertEquals(0, priorityTask.sequence());
    }

    @Test
    void callableWithNegativePriority() {
        Callable<String> callable = () -> "Test";
        PriorityCallable<String> priorityTask = new PriorityCallable<>(callable, -10, 5);

        assertEquals(-10, priorityTask.priority());
        assertEquals(5, priorityTask.sequence());
    }

    @Test
    void callableWithHighSequence() {
        Callable<String> callable = () -> "Test";
        PriorityCallable<String> priorityTask = new PriorityCallable<>(callable, 5, 999999);

        assertEquals(999999, priorityTask.sequence());
    }

    @Test
    void callableEqualsWithNullTask() {
        Callable<String> callable = () -> "Test";
        PriorityCallable<String> priorityTask = new PriorityCallable<>(callable, 5, 5);

        assertNotEquals(priorityTask, null);
    }

    @Test
    void callableEqualsWithDifferentClass() {
        Callable<String> callable = () -> "Test";
        PriorityCallable<String> priorityTask = new PriorityCallable<>(callable, 5, 5);

        assertNotEquals(priorityTask, "Not a PriorityCallable");
    }
}