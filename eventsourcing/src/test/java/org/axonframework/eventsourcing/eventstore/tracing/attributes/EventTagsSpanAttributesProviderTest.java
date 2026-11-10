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

package org.axonframework.eventsourcing.eventstore.tracing.attributes;

import org.axonframework.eventsourcing.eventstore.TagResolver;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.QualifiedName;
import org.axonframework.messaging.eventhandling.EventMessage;
import org.axonframework.messaging.eventhandling.GenericEventMessage;
import org.axonframework.messaging.eventstreaming.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventTagsSpanAttributesProviderTest {
    @Test
    void providesTagsAsAttributes() {
        TagResolver tagResolver = message -> Set.of(new Tag("foo", "bar"));
        EventTagsSpanAttributesProvider provider =
                new EventTagsSpanAttributesProvider(tagResolver);
        EventMessage event = new GenericEventMessage(new MessageType(new QualifiedName("org", "event"), "1.0"), "payload");

        assertEquals(Map.of(EventTagsSpanAttributesProvider.EVENT_TAG_PREFIX + "foo", "bar"),
                provider.provideForMessage(event, null));
    }

    @Test
    void filtersAllowedKeys() {
        TagResolver tagResolver = message -> Set.of(new Tag("foo", "bar"), new Tag("baz", "qux"));
        EventTagsSpanAttributesProvider provider =
                new EventTagsSpanAttributesProvider(tagResolver, Set.of("foo"));
        EventMessage event = new GenericEventMessage(new MessageType(new QualifiedName("org", "event"), "1.0"), "payload");

        assertEquals(Map.of(EventTagsSpanAttributesProvider.EVENT_TAG_PREFIX + "foo", "bar"),
                provider.provideForMessage(event, null));
    }

}
