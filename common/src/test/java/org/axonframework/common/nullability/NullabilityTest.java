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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.common.nullability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class for the {@link Nullability} enum to achieve 100% coverage.
 */
class NullabilityTest {

    @Test
    void enumValuesExist() {
        assertEquals(3, Nullability.values().length);
        assertEquals(Nullability.NULLABLE, Nullability.valueOf("NULLABLE"));
        assertEquals(Nullability.NON_NULL, Nullability.valueOf("NON_NULL"));
        assertEquals(Nullability.UNKNOWN, Nullability.valueOf("UNKNOWN"));
    }
}