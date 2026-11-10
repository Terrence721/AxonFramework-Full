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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetadataTest {
    @Test
    void emptyInstanceIsEmpty() {
        Metadata metadata = Metadata.emptyInstance();

        assertTrue(metadata.isEmpty());
        assertFalse(metadata.containsKey("key"));
        assertEquals(null, metadata.get("key"));
    }

    @Test
    void fromCreatesNewMetadataWithEntries() {
        Metadata metadata = Metadata.from(java.util.Map.of("a", "1"));

        assertEquals("1", metadata.get("a"));
        assertEquals(1, metadata.size());
    }

    @Test
    void andReturnsNewMetadataWithoutMutatingOriginal() {
        Metadata original = Metadata.from(java.util.Map.of("a", "1"));
        Metadata updated = original.and("b", "2");

        assertEquals("2", updated.get("b"));
        assertFalse(original.containsKey("b"));
    }
}
