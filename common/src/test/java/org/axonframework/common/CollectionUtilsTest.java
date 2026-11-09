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

package org.axonframework.common;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollectionUtilsTest {

    @Test
    void mergeReturnsSecondWhenFirstEmpty() {
        List<String> result = CollectionUtils.merge(new ArrayList<>(), List.of("b"), ArrayList::new);

        assertEquals(List.of("b"), result);
    }

    @Test
    void mergeReturnsNullWhenBothInputsNull() {
        List<String> result = CollectionUtils.merge(null, null, ArrayList::new);

        assertNull(result);
    }

    @Test
    void asCollectionConvertsArrayToList() {
        Collection<String> result = CollectionUtils.asCollection(new String[]{"a", "b"});

        assertEquals(List.of("a", "b"), result);
    }

    @Test
    void intersectReturnsCommonElementsInFirstCollectionOrder() {
        List<String> result = CollectionUtils.intersect(List.of("a", "b", "c"), List.of("c", "a"), ArrayList::new);

        assertEquals(List.of("a", "c"), result);
    }

    @Test
    void mergeCombinesNonEmptyCollections() {
        List<String> result = CollectionUtils.merge(List.of("a"), List.of("b"), ArrayList::new);

        assertEquals(List.of("a", "b"), result);
    }

    @Test
    void asCollectionHandlesNullStreamIterableAndObject() {
        assertEquals(List.of(), CollectionUtils.asCollection(null));
        assertEquals(List.of("a", "b"), CollectionUtils.asCollection(List.of("a", "b").stream()));
        assertEquals(List.of("x"), CollectionUtils.asCollection("x"));
    }

    @Test
    void mapWithAddsNewEntryToCopiedMap() {
        Map<String, Integer> base = new HashMap<>();
        base.put("a", 1);

        Map<String, Integer> result = CollectionUtils.mapWith(base, "b", 2);

        assertEquals(Map.of("a", 1, "b", 2), result);
        assertEquals(Map.of("a", 1), base);
    }
}
