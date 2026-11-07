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

package org.axonframework.conversion.jackson2;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ObjectNodeToJsonNodeConverterTest {

    @Test
    void passesInputThrough() {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("value", "hello");

        ObjectNodeToJsonNodeConverter converter = new ObjectNodeToJsonNodeConverter();

        assertEquals(node, converter.convert(node));
    }

    @Test
    void nullInputReturnsNull() {
        ObjectNodeToJsonNodeConverter converter = new ObjectNodeToJsonNodeConverter();

        assertNull(converter.convert(null));
    }
}
