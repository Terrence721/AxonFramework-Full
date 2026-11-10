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

package org.axonframework.messaging.eventhandling.conversion;

import org.axonframework.common.infra.ComponentDescriptor;
import org.axonframework.conversion.Converter;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.conversion.DelegatingMessageConverter;
import org.axonframework.messaging.core.conversion.MessageConverter;
import org.axonframework.messaging.eventhandling.EventMessage;
import org.axonframework.messaging.eventhandling.GenericEventMessage;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link DelegatingEventConverter} and the default methods of {@link EventConverter}.
 */
class DelegatingEventConverterTest {

    private static final MessageType TYPE = new MessageType("test.event");

    /** Converts to String via toString and Integer via parsing, recording the requested target types. */
    private static final class RecordingConverter implements Converter {

        final List<Type> requested = new ArrayList<>();

        @Nullable
        @Override
        @SuppressWarnings("unchecked")
        public <T> T convert(@Nullable Object input, Type targetType) {
            requested.add(targetType);
            if (input == null) {
                return null;
            }
            if (targetType == String.class) {
                return (T) input.toString();
            }
            if (targetType == Integer.class) {
                return (T) Integer.valueOf(input.toString());
            }
            throw new IllegalArgumentException("Unsupported " + targetType);
        }

        @Override
        public void describeTo(ComponentDescriptor descriptor) {
            descriptor.describeProperty("type", "recording");
        }
    }

    @Test
    void constructorsRejectNullDelegates() {
        assertThrows(NullPointerException.class, () -> new DelegatingEventConverter((Converter) null));
        assertThrows(NullPointerException.class, () -> new DelegatingEventConverter((MessageConverter) null));
    }

    @Test
    void plainConverterIsWrappedInADelegatingMessageConverter() {
        RecordingConverter converter = new RecordingConverter();

        MessageConverter delegate = new DelegatingEventConverter(converter).delegate();

        DelegatingMessageConverter wrapper = assertInstanceOf(DelegatingMessageConverter.class, delegate);
        assertSame(converter, wrapper.delegate());
    }

    @Test
    void messageConverterIsUsedAsIsWithoutRewrapping() {
        MessageConverter messageConverter = new DelegatingMessageConverter(new RecordingConverter());

        assertSame(messageConverter, new DelegatingEventConverter(messageConverter).delegate());
    }

    @Test
    void convertDelegatesIncludingNullInput() {
        RecordingConverter converter = new RecordingConverter();
        DelegatingEventConverter testSubject = new DelegatingEventConverter(converter);

        assertEquals("5", testSubject.convert(5, String.class));
        assertNull(testSubject.convert(null, String.class));
        assertEquals(List.of(String.class, String.class), converter.requested);
    }

    @Test
    void convertPayloadConvertsThePayloadOfTheEvent() {
        RecordingConverter converter = new RecordingConverter();
        DelegatingEventConverter testSubject = new DelegatingEventConverter(converter);
        EventMessage event = new GenericEventMessage(TYPE, "42");

        Integer result = testSubject.convertPayload(event, Integer.class);

        assertEquals(42, result);
        assertEquals(List.of(Integer.class), converter.requested);
    }

    @Test
    void convertPayloadWithClassOverloadAndTypeOverloadAgree() {
        DelegatingEventConverter testSubject = new DelegatingEventConverter(new RecordingConverter());
        EventMessage event = new GenericEventMessage(TYPE, "7");

        Integer viaClass = testSubject.convertPayload(event, Integer.class);
        Integer viaType = testSubject.convertPayload(event, (Type) Integer.class);

        assertEquals(viaClass, viaType);
    }

    @Test
    void convertPayloadToAssignableTypeSkipsTheConverter() {
        RecordingConverter converter = new RecordingConverter();
        DelegatingEventConverter testSubject = new DelegatingEventConverter(converter);
        EventMessage event = new GenericEventMessage(TYPE, "same");

        assertEquals("same", testSubject.convertPayload(event, String.class));
        assertEquals(List.of(), converter.requested);
    }

    @Test
    void convertEventReturnsNewEventWithConvertedPayloadAndSameIdentity() {
        RecordingConverter converter = new RecordingConverter();
        DelegatingEventConverter testSubject = new DelegatingEventConverter(converter);
        EventMessage event = new GenericEventMessage(TYPE, "13");

        EventMessage converted = testSubject.convertEvent(event, Integer.class);

        assertEquals(13, converted.payload());
        assertEquals(event.identifier(), converted.identifier());
        assertEquals(event.type(), converted.type());
        assertEquals(event.timestamp(), converted.timestamp());
        assertEquals("13", event.payload());
    }

    @Test
    void convertEventWithTypeOverloadMatchesClassOverload() {
        DelegatingEventConverter testSubject = new DelegatingEventConverter(new RecordingConverter());
        EventMessage event = new GenericEventMessage(TYPE, "99");

        assertEquals(99, testSubject.convertEvent(event, (Type) Integer.class).payload());
        assertEquals(99, testSubject.convertEvent(event, Integer.class).payload());
    }

    @Test
    void conversionFailuresPropagate() {
        DelegatingEventConverter testSubject = new DelegatingEventConverter(new RecordingConverter());
        EventMessage event = new GenericEventMessage(TYPE, "not-a-number");

        assertThrows(RuntimeException.class, () -> testSubject.convertPayload(event, Integer.class));
    }

    @Test
    void describeToDescribesTheDelegateUnderMessageConverter() {
        MessageConverter messageConverter = new DelegatingMessageConverter(new RecordingConverter());
        DelegatingEventConverter testSubject = new DelegatingEventConverter(messageConverter);
        List<String> described = new ArrayList<>();
        ComponentDescriptor descriptor = (ComponentDescriptor) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{ComponentDescriptor.class}, (proxy, method, args) -> {
                    if (method.getName().equals("describeProperty") && args.length == 2) {
                        described.add(args[0] + "=" + (args[1] == messageConverter ? "delegate" : args[1]));
                    }
                    return null;
                });

        testSubject.describeTo(descriptor);

        assertEquals(List.of("messageConverter=delegate"), described);
    }
}
