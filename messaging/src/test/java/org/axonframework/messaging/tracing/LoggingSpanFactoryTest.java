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

import org.axonframework.common.infra.FilesystemStyleComponentDescriptor;
import org.axonframework.messaging.core.Message;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.eventhandling.GenericEventMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

import static org.axonframework.messaging.tracing.attributes.TracingTestContexts.inStartedContext;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link LoggingSpanFactory} and the spans and scopes it creates.
 */
class LoggingSpanFactoryTest {

    private static final Message MESSAGE = new GenericEventMessage(new MessageType("test.event"), "payload");
    private static final Message LINKED = new GenericEventMessage(new MessageType("test.linked"), "linked");

    @FunctionalInterface
    private interface SpanCreation {

        Span create(LoggingSpanFactory factory);
    }

    private record Creation(String name, SpanCreation creation) {

        @Override
        public String toString() {
            return name;
        }
    }

    static Stream<Creation> creations() {
        return Stream.of(
                new Creation("dispatch", f -> f.createDispatchSpan("op", MESSAGE, null)),
                new Creation("handler", f -> f.createHandlerSpan("op", MESSAGE, null)),
                new Creation("context parent handler", f -> f.createContextParentHandlerSpan("op", MESSAGE, null)),
                new Creation("linked handler", f -> f.createLinkedHandlerSpan("op", MESSAGE, LINKED, null)),
                new Creation("internal", f -> f.createInternalSpan("op", null)),
                new Creation("disconnected handler", f -> f.createDisconnectedHandlerSpan("op", MESSAGE, null)),
                new Creation("root", f -> f.createRootSpan("op", null))
        );
    }

    @Test
    void isASingletonWithAPrivateConstructor() {
        Constructor<?>[] constructors = LoggingSpanFactory.class.getDeclaredConstructors();

        assertSame(LoggingSpanFactory.INSTANCE, LoggingSpanFactory.INSTANCE);
        assertTrue(Modifier.isFinal(LoggingSpanFactory.class.getModifiers()));
        assertEquals(1, constructors.length);
        assertTrue(Modifier.isPrivate(constructors[0].getModifiers()));
    }

    @ParameterizedTest(name = "{0} span")
    @MethodSource("creations")
    void everyKindOfSpanStartsAScopeThatCanBeClosedOnce(Creation creation) {
        Span span = creation.creation().create(LoggingSpanFactory.INSTANCE);

        SpanScope scope = span.start();

        assertSame(span, scope.span());
        assertFalse(scope.isClosed());
        scope.close();
        assertTrue(scope.isClosed());
        assertDoesNotThrow(scope::close);
        assertTrue(scope.isClosed());
    }

    @ParameterizedTest(name = "{0} span")
    @MethodSource("creations")
    void everySpanIsASeparateInstance(Creation creation) {
        Span first = creation.creation().create(LoggingSpanFactory.INSTANCE);
        Span second = creation.creation().create(LoggingSpanFactory.INSTANCE);

        assertTrue(first != second);
    }

    @Test
    void spanOperationsAreChainableAndLeaveMessagesUntouched() {
        Span span = LoggingSpanFactory.INSTANCE.createRootSpan("op", null);

        assertSame(span, span.addAttribute("key", "value"));
        assertSame(span, span.recordException(new IllegalStateException("boom")));
        assertSame(MESSAGE, span.propagateContext(MESSAGE));
    }

    @Test
    void scopesRunOperationsTransparently() {
        SpanScope scope = LoggingSpanFactory.INSTANCE.createRootSpan("op", null).start();

        assertEquals("result", scope.within(() -> "result"));
        assertDoesNotThrow(() -> scope.within(() -> {
        }));
    }

    @Test
    void spansCanBeCreatedWhileHandlingAMessageInAContext() {
        assertDoesNotThrow(() -> inStartedContext(context -> {
            LoggingSpanFactory.INSTANCE.createDispatchSpan("dispatch", MESSAGE, context).start().close();
            LoggingSpanFactory.INSTANCE.createInternalSpan("internal", context).start().close();
            return null;
        }));
    }

    @Test
    void spansCoverTheLifecycleOfAContext() {
        SpanScope scope = inStartedContext(context ->
                LoggingSpanFactory.INSTANCE.createHandlerSpan("handler", MESSAGE, context).coverLifecycle(context));

        assertTrue(scope.isClosed());
    }

    @Test
    void describesNothing() {
        FilesystemStyleComponentDescriptor descriptor = new FilesystemStyleComponentDescriptor();

        LoggingSpanFactory.INSTANCE.describeTo(descriptor);

        assertEquals(new FilesystemStyleComponentDescriptor().describe(), descriptor.describe());
    }
}
