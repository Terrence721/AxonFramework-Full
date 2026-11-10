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

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ByteArrayToJsonNodeConverterTest {

    @Test
    void convertsBytesToNode() {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree("{\"value\":\"hello\"}".getBytes(StandardCharsets.UTF_8));
        ByteArrayToJsonNodeConverter converter = new ByteArrayToJsonNodeConverter(mapper);
        assertEquals(node.get("value").stringValue(), converter.convert("{\"value\":\"hello\"}".getBytes(StandardCharsets.UTF_8)).get("value").stringValue());
    }

    @Test
    void nullInputReturnsNull() {
        ObjectMapper mapper = new ObjectMapper();
        ByteArrayToJsonNodeConverter converter = new ByteArrayToJsonNodeConverter(mapper);

        assertNull(converter.convert(null));
    }
}
