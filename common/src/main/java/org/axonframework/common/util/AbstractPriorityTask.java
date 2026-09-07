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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.common.util;

import java.util.Objects;

/**
 * Base class for {@link PriorityTask} implementations that wrap a task (such as a {@link Runnable} or
 * {@link java.util.concurrent.Callable}) together with a {@code priority} and {@code sequence}.
 * <p>
 * Package-private - this exists purely to share the identical {@code equals}/{@code hashCode}/{@code toString}
 * logic {@link PriorityCallable} and {@link PriorityRunnable} would otherwise duplicate, not as a public
 * extension point.
 *
 * @param <W> The type of task being wrapped.
 */
abstract class AbstractPriorityTask<W> implements PriorityTask {

    protected final W task;
    private final long priority;
    private final long sequence;

    /**
     * Construct an {@code AbstractPriorityTask} wrapping the given {@code task}.
     *
     * @param task     The task to wrap.
     * @param priority The priority of the {@code task}, dedicating the order among tasks.
     * @param sequence The sequence of the {@code task}, dedicating the order among equal {@code priority} tasks.
     */
    protected AbstractPriorityTask(W task, long priority, long sequence) {
        this.task = task;
        this.priority = priority;
        this.sequence = sequence;
    }

    @Override
    public long priority() {
        return priority;
    }

    @Override
    public long sequence() {
        return sequence;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractPriorityTask<?> that = (AbstractPriorityTask<?>) o;
        return priority == that.priority && sequence == that.sequence && Objects.equals(task, that.task);
    }

    @Override
    public int hashCode() {
        return Objects.hash(task, priority, sequence);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "task=" + task +
                ", priority=" + priority +
                ", sequence=" + sequence +
                '}';
    }
}
