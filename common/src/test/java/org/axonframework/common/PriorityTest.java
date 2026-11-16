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

package org.axonframework.common;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for the {@link Priority} annotation to achieve 100% coverage.
 */
class PriorityTest {

    @Test
    void annotationIsPresent() {
        assertNotNull(Priority.class);
    }

    @Test
    void annotationHasInheritedAnnotation() {
        assertTrue(Priority.class.isAnnotationPresent(Inherited.class));
    }

    @Test
    void annotationHasTargetAnnotation() {
        assertTrue(Priority.class.isAnnotationPresent(Target.class));
    }

    @Test
    void annotationHasRetentionAnnotation() {
        assertTrue(Priority.class.isAnnotationPresent(Retention.class));
    }

    @Test
    void annotationCanBeRetrievedFromClass() {
        Priority annotation = TestClassWithPriority.class.getAnnotation(Priority.class);
        assertNotNull(annotation);
        assertEquals(Priority.NEUTRAL, annotation.value());
    }

    @Test
    void annotationCanBeRetrievedFromSubclass() {
        Priority annotation = SubclassWithPriority.class.getAnnotation(Priority.class);
        assertNotNull(annotation);
        assertEquals(Priority.NEUTRAL, annotation.value());
    }

    @Test
    void annotationWithCustomValue() {
        Priority annotation = CustomPriorityClass.class.getAnnotation(Priority.class);
        assertNotNull(annotation);
        assertEquals(42, annotation.value());
    }

    @Test
    void annotationWithHighValue() {
        Priority annotation = HighPriorityClass.class.getAnnotation(Priority.class);
        assertNotNull(annotation);
        assertEquals(Priority.HIGH, annotation.value());
    }

    @Test
    void annotationWithFirstValue() {
        Priority annotation = FirstPriorityClass.class.getAnnotation(Priority.class);
        assertNotNull(annotation);
        assertEquals(Priority.FIRST, annotation.value());
    }

    @Test
    void annotationWithLowValue() {
        Priority annotation = LowPriorityClass.class.getAnnotation(Priority.class);
        assertNotNull(annotation);
        assertEquals(Priority.LOW, annotation.value());
    }

    @Test
    void annotationWithLastValue() {
        Priority annotation = LastPriorityClass.class.getAnnotation(Priority.class);
        assertNotNull(annotation);
        assertEquals(Priority.LAST, annotation.value());
    }

    // Helper classes for testing
    @Priority
    static class TestClassWithPriority {
    }

    static class SubclassWithPriority extends TestClassWithPriority {
    }

    @Priority(42)
    static class CustomPriorityClass {
    }

    @Priority(Priority.HIGH)
    static class HighPriorityClass {
    }

    @Priority(Priority.FIRST)
    static class FirstPriorityClass {
    }

    @Priority(Priority.LOW)
    static class LowPriorityClass {
    }

    @Priority(Priority.LAST)
    static class LastPriorityClass {
    }
}