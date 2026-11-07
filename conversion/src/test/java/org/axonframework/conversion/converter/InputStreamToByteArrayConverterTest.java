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

package org.axonframework.conversion.converter;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InputStreamToByteArrayConverterTest {

    @Test
    void readsStream() {
        InputStreamToByteArrayConverter converter = new InputStreamToByteArrayConverter();
        ByteArrayInputStream input = new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8));

        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), converter.convert(input));
    }

    @Test
    void nullInputReturnsNull() {
        InputStreamToByteArrayConverter converter = new InputStreamToByteArrayConverter();

        assertNull(converter.convert(null));
    }
}
