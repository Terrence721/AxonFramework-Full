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

package org.axonframework.conversion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChainingContentTypeConverterTest {

    @Test
    void sameTypeIsConvertible() {
        ChainingContentTypeConverter converter = new ChainingContentTypeConverter();

        assertTrue(converter.canConvert(String.class, String.class));
    }

    @Test
    void unknownTypeMismatchIsNotConvertible() {
        ChainingContentTypeConverter converter = new ChainingContentTypeConverter();

        assertFalse(converter.canConvert(String.class, Integer.class));
    }

    @Test
    void convertReturnsNullForNullInput() {
        ChainingContentTypeConverter converter = new ChainingContentTypeConverter();

        assertNull(converter.convert(null, String.class));
    }

    @Test
    void convertReturnsInputForIdenticalType() {
        ChainingContentTypeConverter converter = new ChainingContentTypeConverter();

        assertEquals("value", converter.convert("value", String.class));
    }
}
