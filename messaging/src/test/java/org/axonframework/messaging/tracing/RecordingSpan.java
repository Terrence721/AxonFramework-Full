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

package org.axonframework.messaging.tracing;

import org.axonframework.messaging.core.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * A {@link Span} that records, in order, everything that happens to it and to the scopes it starts. Used to test the
 * default methods of {@link Span} and {@link SpanScope}.
 */
final class RecordingSpan implements Span {

    private final List<String> events = Collections.synchronizedList(new ArrayList<>());
    private final List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());
    private final List<Scope> scopes = Collections.synchronizedList(new ArrayList<>());

    /**
     * The recorded events, for example {@code start}, {@code exception:boom} and {@code close}.
     */
    List<String> events() {
        return events;
    }

    List<Throwable> exceptions() {
        return exceptions;
    }

    /**
     * The most recently started scope.
     */
    Scope scope() {
        return scopes.get(scopes.size() - 1);
    }

    int startedScopes() {
        return scopes.size();
    }

    @Override
    public SpanScope start() {
        events.add("start");
        Scope scope = new Scope(this);
        scopes.add(scope);
        return scope;
    }

    @Override
    public Span addAttribute(String key, String value) {
        events.add("attribute:" + key + "=" + value);
        return this;
    }

    @Override
    public Span recordException(Throwable t) {
        events.add("exception:" + t.getMessage());
        exceptions.add(t);
        return this;
    }

    @Override
    public <M extends Message> M propagateContext(M message) {
        events.add("propagate");
        return message;
    }

    /**
     * The scope handed out by {@link #start()}.
     */
    static final class Scope implements SpanScope {

        private final RecordingSpan span;
        private boolean closed;
        private int closeCalls;
        private int withinCalls;
        private int depth;

        private Scope(RecordingSpan span) {
            this.span = span;
        }

        int closeCalls() {
            return closeCalls;
        }

        int withinCalls() {
            return withinCalls;
        }

        /**
         * How many {@link #within(Supplier)} invocations are currently active.
         */
        int depth() {
            return depth;
        }

        @Override
        public Span span() {
            return span;
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

        @Override
        public void close() {
            closeCalls++;
            if (!closed) {
                closed = true;
                span.events.add("close");
            }
        }

        @Override
        public <T> T within(Supplier<T> operation) {
            withinCalls++;
            depth++;
            try {
                return operation.get();
            } finally {
                depth--;
            }
        }
    }
}
