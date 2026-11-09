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
package org.axonframework.messaging.commandhandling;

import org.axonframework.common.infra.ComponentDescriptor;
import org.axonframework.conversion.Converter;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.Metadata;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link GenericCommandResultMessage}.
 */
class GenericCommandResultMessageTest {

    private static final MessageType TYPE = new MessageType("test.result");

    private static final Converter TO_NUMBER = new Converter() {
        @SuppressWarnings("unchecked")
        @Override
        public <T> @Nullable T convert(@Nullable Object input, Type targetType) {
            return (T) Integer.valueOf(7);
        }

        @Override
        public void describeTo(ComponentDescriptor descriptor) {
        }
    };

    @Test
    void exposesTypePayloadAndMetadata() {
        GenericCommandResultMessage message = new GenericCommandResultMessage(TYPE, "ok", Metadata.with("a", "1"));

        assertEquals(TYPE, message.type());
        assertEquals("ok", message.payload());
        assertEquals("1", message.metadata().get("a"));
    }

    @Test
    void metadataChangesKeepTheIdentifierAndPayload() {
        GenericCommandResultMessage message = new GenericCommandResultMessage(TYPE, "ok", Metadata.with("a", "1"));

        CommandResultMessage replaced = message.withMetadata(Metadata.with("b", "2"));
        CommandResultMessage merged = message.andMetadata(Metadata.with("b", "2"));

        assertEquals(Metadata.with("b", "2"), replaced.metadata());
        assertEquals("1", merged.metadata().get("a"));
        assertEquals("2", merged.metadata().get("b"));
        assertEquals(message.identifier(), replaced.identifier());
        assertEquals(message.identifier(), merged.identifier());
        assertEquals("ok", merged.payload());
    }

    @Test
    void withConvertedPayloadReturnsTheSameInstanceWhenTheTypeAlreadyMatches() {
        GenericCommandResultMessage message = new GenericCommandResultMessage(TYPE, "ok");

        assertSame(message, message.withConvertedPayload(String.class, new Converter() {
            @SuppressWarnings("unchecked")
            @Override
            public <T> @Nullable T convert(@Nullable Object input, Type targetType) {
                return (T) input;
            }

            @Override
            public void describeTo(ComponentDescriptor descriptor) {
            }
        }));
    }

    @Test
    void withConvertedPayloadKeepsIdentifierAndMetadata() {
        GenericCommandResultMessage message = new GenericCommandResultMessage(TYPE, "ok", Metadata.with("a", "1"));

        CommandResultMessage converted = message.withConvertedPayload(Integer.class, TO_NUMBER);

        assertEquals(7, converted.payload());
        assertEquals(message.identifier(), converted.identifier());
        assertEquals("1", converted.metadata().get("a"));
    }

    @Test
    void toStringNamesTheMessageType() {
        assertTrue(new GenericCommandResultMessage(TYPE, "ok").toString().contains("GenericCommandResultMessage"));
    }
}
