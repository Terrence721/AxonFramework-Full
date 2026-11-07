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

package org.axonframework.eventsourcing;

import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.QualifiedName;
import org.axonframework.messaging.eventhandling.EventMessage;
import org.axonframework.messaging.eventhandling.GenericEventMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EventSourcedEntityFactoryTest {

    @Test
    void fromNoArgumentCreatesEntity() {
        EventSourcedEntityFactory<String, String> factory = EventSourcedEntityFactory.fromNoArgument(() -> "created");

        assertEquals("created", factory.create("id", null, null));
    }

    @Test
    void fromIdentifierCreatesEntityFromIdentifier() {
        EventSourcedEntityFactory<String, String> factory = EventSourcedEntityFactory.fromIdentifier(id -> "created-" + id);

        assertEquals("created-42", factory.create("42", null, null));
    }

    @Test
    void fromEventMessageReturnsNullWhenFirstEventMissing() {
        EventSourcedEntityFactory<String, String> factory = EventSourcedEntityFactory.fromEventMessage((id, event) -> "created");

        assertNull(factory.create("id", null, null));
    }

    @Test
    void fromEventMessageCreatesEntityWhenFirstEventPresent() {
        EventSourcedEntityFactory<String, String> factory = EventSourcedEntityFactory.fromEventMessage((id, event) -> "created-" + event.payload());
        EventMessage event = new GenericEventMessage(new MessageType(new QualifiedName("org", "event"), "1.0"), "event");

        assertEquals("created-event", factory.create("id", event, null));
    }
}
