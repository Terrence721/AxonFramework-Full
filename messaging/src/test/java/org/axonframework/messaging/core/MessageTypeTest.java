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

package org.axonframework.messaging.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageTypeTest {
    @Test
    void constructorWithQualifiedParts() {
        MessageType type = new MessageType("org", "event", MessageType.DEFAULT_VERSION);

        assertEquals("org", type.qualifiedName().namespace());
        assertEquals("event", type.qualifiedName().localName());
        assertEquals(MessageType.DEFAULT_VERSION, type.version());
    }

    @Test
    void constructorFromClassUsesClassNameAndVersion() {
        MessageType type = new MessageType(String.class, "1.0");

        assertEquals("java.lang", type.qualifiedName().namespace());
        assertEquals("String", type.qualifiedName().localName());
        assertEquals("1.0", type.version());
    }
}
