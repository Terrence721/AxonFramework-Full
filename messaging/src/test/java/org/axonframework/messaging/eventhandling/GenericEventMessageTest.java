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
package org.axonframework.messaging.eventhandling;

import org.axonframework.common.infra.ComponentDescriptor;
import org.axonframework.conversion.Converter;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.Metadata;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link GenericEventMessage}.
 */
class GenericEventMessageTest {

    private static final MessageType TYPE = new MessageType("test.event");

    @Test
    void constructorsExposeTypePayloadAndMetadata() {
        GenericEventMessage message = new GenericEventMessage(TYPE, "payload", Map.of("k", "v"));

        assertEquals(TYPE, message.type());
        assertEquals("payload", message.payload());
        assertEquals("v", message.metadata().get("k"));
    }

    @Test
    void explicitIdentifierAndTimestampAreRetained() {
        Instant timestamp = Instant.parse("2026-01-01T00:00:00Z");

        GenericEventMessage message = new GenericEventMessage("id-1", TYPE, "payload", Map.of(), timestamp);

        assertEquals("id-1", message.identifier());
        assertEquals(timestamp, message.timestamp());
    }

    @Test
    void timestampIsComputedOnceAndCached() {
        AtomicInteger calls = new AtomicInteger();
        GenericEventMessage message = new GenericEventMessage(
                new GenericEventMessage(TYPE, "payload"),
                () -> {
                    calls.incrementAndGet();
                    return Instant.now();
                }
        );

        assertSame(message.timestamp(), message.timestamp());
        assertEquals(1, calls.get());
    }

    @Test
    void withMetadataReplacesMetadataAndKeepsTimestampAndIdentifier() {
        GenericEventMessage message = new GenericEventMessage(TYPE, "payload", Map.of("a", "1"));

        EventMessage replaced = message.withMetadata(Map.of("b", "2"));

        assertEquals(Metadata.from(Map.of("b", "2")), replaced.metadata());
        assertEquals(message.identifier(), replaced.identifier());
        assertEquals(message.timestamp(), replaced.timestamp());
    }

    @Test
    void withMetadataReturnsSameInstanceWhenUnchanged() {
        GenericEventMessage message = new GenericEventMessage(TYPE, "payload", Map.of("a", "1"));

        assertSame(message, message.withMetadata(Map.of("a", "1")));
    }

    @Test
    void andMetadataMergesAndKeepsTimestampAndIdentifier() {
        GenericEventMessage message = new GenericEventMessage(TYPE, "payload", Map.of("a", "1"));

        EventMessage merged = message.andMetadata(Map.of("b", "2"));

        assertEquals("1", merged.metadata().get("a"));
        assertEquals("2", merged.metadata().get("b"));
        assertEquals(message.identifier(), merged.identifier());
        assertEquals(message.timestamp(), merged.timestamp());
    }

    @Test
    void andMetadataReturnsSameInstanceForEmptyOrUnchangedMetadata() {
        GenericEventMessage message = new GenericEventMessage(TYPE, "payload", Map.of("a", "1"));

        assertSame(message, message.andMetadata(Map.of()));
        assertSame(message, message.andMetadata(Map.of("a", "1")));
    }

    @Test
    void withConvertedPayloadReturnsSameInstanceWhenAlreadyOfTheTargetType() {
        GenericEventMessage message = new GenericEventMessage(TYPE, "payload");

        assertSame(message, message.withConvertedPayload(String.class, new IdentityConverter()));
    }

    @Test
    void withConvertedPayloadKeepsIdentifierMetadataAndTimestamp() {
        GenericEventMessage message = new GenericEventMessage(TYPE, "42", Map.of("a", "1"));

        EventMessage converted = message.withConvertedPayload(Integer.class, new IntegerConverter());

        assertNotSame(message, converted);
        assertEquals(42, converted.payload());
        assertEquals(message.identifier(), converted.identifier());
        assertEquals(message.metadata(), converted.metadata());
        assertEquals(message.timestamp(), converted.timestamp());
    }

    @Test
    void withConverterKeepsTheTimestamp() {
        GenericEventMessage message = new GenericEventMessage(TYPE, "payload");

        GenericEventMessage withConverter = message.withConverter(new IdentityConverter());

        assertEquals(message.timestamp(), withConverter.timestamp());
        assertEquals(message.identifier(), withConverter.identifier());
    }

    @Test
    void toStringMentionsTheMessageKindAndTimestamp() {
        GenericEventMessage message = new GenericEventMessage(TYPE, "payload");

        assertTrue(message.toString().startsWith("GenericEventMessage"));
        assertTrue(message.toString().contains("timestamp='"));
    }

    private static class IdentityConverter implements Converter {

        @Override
        @SuppressWarnings("unchecked")
        public <T> T convert(Object input, Type targetType) {
            return (T) input;
        }

        @Override
        public void describeTo(ComponentDescriptor descriptor) {
        }
    }

    private static class IntegerConverter implements Converter {

        @Override
        public void describeTo(ComponentDescriptor descriptor) {
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T convert(Object input, Type targetType) {
            return (T) Integer.valueOf(input.toString());
        }
    }
}
