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

package org.axonframework.messaging.eventhandling.tracing;

import org.axonframework.common.infra.ComponentDescriptor;
import org.axonframework.messaging.core.Message;
import org.axonframework.messaging.core.unitofwork.ProcessingContext;
import org.axonframework.messaging.tracing.Span;
import org.axonframework.messaging.tracing.SpanFactory;
import org.axonframework.messaging.tracing.SpanScope;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * A {@link SpanFactory} recording which kind of span was created for which operation, and everything that happened to
 * those spans afterwards.
 */
final class RecordingSpanFactory implements SpanFactory {

    final List<String> created = Collections.synchronizedList(new ArrayList<>());
    final List<RecordingSpan> spans = Collections.synchronizedList(new ArrayList<>());

    private Span record(String kind, String operationName) {
        created.add(kind + ":" + operationName);
        RecordingSpan span = new RecordingSpan();
        spans.add(span);
        return span;
    }

    @Override
    public Span createDispatchSpan(String operationName, Message message, @Nullable ProcessingContext context) {
        return record("dispatch", operationName);
    }

    @Override
    public Span createHandlerSpan(String operationName, Message message, @Nullable ProcessingContext context) {
        return record("handler", operationName);
    }

    @Override
    public Span createContextParentHandlerSpan(String operationName, Message message,
                                               @Nullable ProcessingContext context) {
        return record("contextParentHandler", operationName);
    }

    @Override
    public Span createLinkedHandlerSpan(String operationName, Message message, Message linkedMessage,
                                        @Nullable ProcessingContext context) {
        return record("linkedHandler", operationName);
    }

    @Override
    public Span createInternalSpan(String operationName, @Nullable ProcessingContext context) {
        return record("internal", operationName);
    }

    @Override
    public Span createDisconnectedHandlerSpan(String operationName, Message message,
                                              @Nullable ProcessingContext context) {
        return record("disconnectedHandler", operationName);
    }

    @Override
    public Span createRootSpan(String operationName, @Nullable ProcessingContext context) {
        return record("root", operationName);
    }

    @Override
    public void describeTo(ComponentDescriptor descriptor) {
        descriptor.describeProperty("type", "recording");
    }

    /**
     * A span recording its attributes, lifecycle and propagation.
     */
    static final class RecordingSpan implements Span {

        final List<String> events = Collections.synchronizedList(new ArrayList<>());
        final List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());
        volatile boolean closed;

        @Override
        public SpanScope start() {
            events.add("start");
            return new SpanScope() {
                @Override
                public Span span() {
                    return RecordingSpan.this;
                }

                @Override
                public boolean isClosed() {
                    return closed;
                }

                @Override
                public void close() {
                    if (!closed) {
                        closed = true;
                        events.add("close");
                    }
                }

                @Override
                public <T> T within(Supplier<T> operation) {
                    return operation.get();
                }
            };
        }

        @Override
        public Span addAttribute(String key, String value) {
            events.add("attribute:" + key + "=" + value);
            return this;
        }

        @Override
        public Span recordException(Throwable t) {
            exceptions.add(t);
            events.add("exception:" + t.getMessage());
            return this;
        }

        @Override
        public <M extends Message> M propagateContext(M message) {
            events.add("propagate");
            return message;
        }
    }
}
