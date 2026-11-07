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

package org.axonframework.conversion.jackson;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonNodeToObjectNodeConverterTest {

    @Test
    void convertsObjectNodeToItself() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("value", "hello");

        JsonNodeToObjectNodeConverter converter = new JsonNodeToObjectNodeConverter();

        assertEquals(node, converter.convert(node));
    }

    @Test
    void nullInputReturnsNull() {
        JsonNodeToObjectNodeConverter converter = new JsonNodeToObjectNodeConverter();

        assertNull(converter.convert(null));
    }

    @Test
    void nonObjectNodeThrowsConversionException() {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode arrayNode = mapper.createArrayNode();

        JsonNodeToObjectNodeConverter converter = new JsonNodeToObjectNodeConverter();

        assertThrows(org.axonframework.conversion.ConversionException.class, () -> converter.convert(arrayNode));
    }
}
