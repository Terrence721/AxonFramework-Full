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
 * limitations
 * under the License.
 */

package org.axonframework.messaging.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class GenericMessageTest {
    @Test
    void messagePreservesTypeAndPayload() {
        MessageType type = new MessageType("example", "Message", MessageType.DEFAULT_VERSION);
        GenericMessage message = new GenericMessage(type, "payload", Map.of("key", "value"));

        assertEquals(type, message.type());
        assertEquals("payload", message.payload());
        assertEquals("value", message.metadata().get("key"));
        assertNotNull(message.identifier());
    }

    @Test
    void withMetadataReturnsNewMessage() {
        GenericMessage original = new GenericMessage(new MessageType("example", "Message", MessageType.DEFAULT_VERSION),
                "payload", Map.of("old", "value"));
        Message updated = original.withMetadata(Map.of("new", "value"));

        assertEquals("value", updated.metadata().get("new"));
        assertFalse(updated.metadata().containsKey("old"));
        assertNull(original.metadata().get("new"));
    }

    @Test
    void andMetadataMergesMetadata() {
        GenericMessage original = new GenericMessage(new MessageType("example", "Message", MessageType.DEFAULT_VERSION),
                "payload", Map.of("old", "value"));
        Message updated = original.andMetadata(Map.of("new", "value"));

        assertEquals("value", updated.metadata().get("old"));
        assertEquals("value", updated.metadata().get("new"));
    }
}
