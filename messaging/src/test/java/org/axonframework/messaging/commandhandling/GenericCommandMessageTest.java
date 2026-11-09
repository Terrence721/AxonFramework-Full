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

import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.Metadata;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link GenericCommandMessage}.
 */
class GenericCommandMessageTest {

    private static final MessageType TYPE = new MessageType("test.command");

    private static GenericCommandMessage withRoutingAndPriority() {
        return new GenericCommandMessage(TYPE, "payload", Metadata.with("a", "1"), "routing", 5);
    }

    @Test
    void defaultsToNoRoutingKeyAndNoPriority() {
        GenericCommandMessage message = new GenericCommandMessage(TYPE, "payload");

        assertEquals(Optional.empty(), message.routingKey());
        assertEquals(OptionalInt.empty(), message.priority());
        assertEquals(TYPE, message.type());
        assertEquals("payload", message.payload());
        assertTrue(message.metadata().isEmpty());
    }

    @Test
    void exposesRoutingKeyAndPriority() {
        GenericCommandMessage message = withRoutingAndPriority();

        assertEquals(Optional.of("routing"), message.routingKey());
        assertEquals(OptionalInt.of(5), message.priority());
    }

    @Test
    void withMetadataReplacesMetadataAndKeepsRoutingKeyAndPriority() {
        CommandMessage result = withRoutingAndPriority().withMetadata(Metadata.with("b", "2"));

        assertEquals(Metadata.with("b", "2"), result.metadata());
        assertEquals(Optional.of("routing"), result.routingKey());
        assertEquals(OptionalInt.of(5), result.priority());
    }

    @Test
    void andMetadataMergesMetadataAndKeepsRoutingKeyAndPriority() {
        CommandMessage result = withRoutingAndPriority().andMetadata(Metadata.with("b", "2"));

        assertEquals("1", result.metadata().get("a"));
        assertEquals("2", result.metadata().get("b"));
        assertEquals(Optional.of("routing"), result.routingKey());
        assertEquals(OptionalInt.of(5), result.priority());
    }

    @Test
    void metadataChangesKeepTheMessageIdentifier() {
        GenericCommandMessage message = withRoutingAndPriority();

        assertEquals(message.identifier(), message.withMetadata(Metadata.with("b", "2")).identifier());
        assertEquals(message.identifier(), message.andMetadata(Metadata.with("b", "2")).identifier());
    }

    @Test
    void withConvertedPayloadReturnsTheSameInstanceWhenTheTypeAlreadyMatches() {
        GenericCommandMessage message = withRoutingAndPriority();

        CommandMessage result = message.withConvertedPayload(String.class, new IdentityConverter());

        assertSame(message, result);
    }

    @Test
    void withConvertedPayloadKeepsRoutingKeyPriorityAndIdentifier() {
        GenericCommandMessage message = withRoutingAndPriority();

        CommandMessage result = message.withConvertedPayload(Integer.class, new ConstantConverter(42));

        assertEquals(42, result.payload());
        assertEquals(message.identifier(), result.identifier());
        assertEquals(Optional.of("routing"), result.routingKey());
        assertEquals(OptionalInt.of(5), result.priority());
    }

    @Test
    void withConverterKeepsRoutingKeyAndPriority() {
        GenericCommandMessage result = withRoutingAndPriority().withConverter(new IdentityConverter());

        assertEquals(Optional.of("routing"), result.routingKey());
        assertEquals(OptionalInt.of(5), result.priority());
    }

    @Test
    void toStringIncludesRoutingKeyAndPriority() {
        String description = withRoutingAndPriority().toString();

        assertTrue(description.contains("GenericCommandMessage"));
        assertTrue(description.contains("routingKey='routing'"));
        assertTrue(description.contains("priority='5'"));
    }

    @Test
    void toStringShowsDefaultsWhenAbsent() {
        String description = new GenericCommandMessage(TYPE, "payload").toString();

        assertTrue(description.contains("routingKey='null'"));
        assertTrue(description.contains("priority='0'"));
    }

    private static class IdentityConverter implements org.axonframework.conversion.Converter {

        @SuppressWarnings("unchecked")
        @Override
        public <T> T convert(Object input, java.lang.reflect.Type targetType) {
            return (T) input;
        }

        @Override
        public void describeTo(org.axonframework.common.infra.ComponentDescriptor descriptor) {
        }
    }

    private static class ConstantConverter extends IdentityConverter {

        private final Object constant;

        private ConstantConverter(Object constant) {
            this.constant = constant;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T convert(Object input, java.lang.reflect.Type targetType) {
            return (T) constant;
        }
    }
}
