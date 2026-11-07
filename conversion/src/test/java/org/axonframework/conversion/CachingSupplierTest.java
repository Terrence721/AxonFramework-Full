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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CachingSupplierTest {

    @Test
    void ofValueReturnsSuppliedValue() {
        CachingSupplier<String> supplier = CachingSupplier.of("value");

        assertEquals("value", supplier.get());
    }

    @Test
    void ofSupplierCachesResult() {
        AtomicInteger invocations = new AtomicInteger();
        Supplier<String> delegate = () -> {
            invocations.incrementAndGet();
            return "value";
        };

        CachingSupplier<String> supplier = CachingSupplier.of(delegate);

        assertEquals("value", supplier.get());
        assertEquals("value", supplier.get());
        assertEquals(1, invocations.get());
    }

    @Test
    void ofSupplierReturnsSameInstanceWhenDelegateIsCachingSupplier() {
        CachingSupplier<String> original = CachingSupplier.of("value");

        CachingSupplier<String> cached = CachingSupplier.of(original);

        assertSame(original, cached);
    }
}
