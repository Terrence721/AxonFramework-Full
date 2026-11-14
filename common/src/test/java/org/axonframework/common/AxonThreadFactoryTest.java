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
 * See the License for the specific language governing permissions and limitations
 * under the License.
 */

package org.axonframework.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AxonThreadFactoryTest {

    @Test
    void newThreadUsesGroupName() {
        AxonThreadFactory factory = new AxonThreadFactory("axon-group");
        Thread thread = factory.newThread(() -> {});
        assertTrue(thread.getName().startsWith("axon-group-"));
        assertEquals(Thread.NORM_PRIORITY, thread.getPriority());
    }

    @Test
    void newThreadUsesThreadGroup() {
        ThreadGroup group = new ThreadGroup("group-from-ref");
        AxonThreadFactory factory = new AxonThreadFactory(group);
        Thread thread = factory.newThread(() -> {});
        assertTrue(thread.getName().startsWith("group-from-ref-"));
        assertEquals(Thread.NORM_PRIORITY, thread.getPriority());
        assertEquals(group, thread.getThreadGroup());
    }

    @Test
    void newThreadUsesGroupNameAndPriority() {
        AxonThreadFactory factory = new AxonThreadFactory(Thread.NORM_PRIORITY, new ThreadGroup("axon-test"));
        Thread thread = factory.newThread(() -> {});
        assertTrue(thread.getName().startsWith("axon-test-"));
        assertEquals(Thread.NORM_PRIORITY, thread.getPriority());
    }

    @Test
    void customPriorityIsApplied() {
        AxonThreadFactory factory = new AxonThreadFactory(Thread.MIN_PRIORITY, new ThreadGroup("low-prio"));
        Thread thread = factory.newThread(() -> {});
        assertEquals(Thread.MIN_PRIORITY, thread.getPriority());
    }

    @Test
    void maxPriorityIsAccepted() {
        AxonThreadFactory factory = new AxonThreadFactory(Thread.MAX_PRIORITY, new ThreadGroup("max-prio"));
        Thread thread = factory.newThread(() -> {});
        assertEquals(Thread.MAX_PRIORITY, thread.getPriority());
    }

    @Test
    void tooLowPriorityThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new AxonThreadFactory(Thread.MIN_PRIORITY - 1, new ThreadGroup("too-low")));
    }

    @Test
    void tooHighPriorityThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new AxonThreadFactory(Thread.MAX_PRIORITY + 1, new ThreadGroup("too-high")));
    }
}
