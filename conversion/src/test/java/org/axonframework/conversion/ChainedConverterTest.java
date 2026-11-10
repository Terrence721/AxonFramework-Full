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
import java.util.Collection;

import org.jspecify.annotations.Nullable;
import org.axonframework.conversion.converter.StringToByteArrayConverter;
import org.axonframework.conversion.converter.ByteArrayToStringConverter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChainedConverterTest {

    @Test
    void delegatesSingleConverter() {
        ContentTypeConverter<String, String> pass = new ContentTypeConverter<>() {
            @Override
            public Class<String> expectedSourceType() {
                return String.class;
            }

            @Override
            public Class<String> targetType() {
                return String.class;
            }

            @Override
            public @Nullable String convert(@Nullable String input) {
                return input;
            }
        };

        ChainedConverter<String, String> converter = new ChainedConverter<>(java.util.List.of(pass));

        assertEquals("value", converter.convert("value"));
    }

    @Test
    void calculateChainConnectsRegisteredCandidates() {
        Collection<ContentTypeConverter<?, ?>> candidates =
                java.util.List.of(new StringToByteArrayConverter(),
                        new ByteArrayToStringConverter());

        ChainedConverter<String, String> converter =
                ChainedConverter.calculateChain(String.class, String.class, candidates);

        assertEquals("value", converter.convert("value"));
        assertTrue(ChainedConverter.canConvert(String.class, String.class, java.util.List.copyOf(candidates)));
    }
}
