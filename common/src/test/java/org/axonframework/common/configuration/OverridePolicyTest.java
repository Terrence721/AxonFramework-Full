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

package org.axonframework.common.configuration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for the {@link OverridePolicy} enum to achieve 100% coverage.
 */
class OverridePolicyTest {

    @Test
    void enumValuesAreCorrect() {
        OverridePolicy[] values = OverridePolicy.values();
        assertNotNull(values);
        assertEquals(3, values.length);
        assertEquals(OverridePolicy.ALLOW, values[0]);
        assertEquals(OverridePolicy.WARN, values[1]);
        assertEquals(OverridePolicy.REJECT, values[2]);
    }

    @Test
    void valueOfWorksCorrectly() {
        assertEquals(OverridePolicy.ALLOW, OverridePolicy.valueOf("ALLOW"));
        assertEquals(OverridePolicy.WARN, OverridePolicy.valueOf("WARN"));
        assertEquals(OverridePolicy.REJECT, OverridePolicy.valueOf("REJECT"));
    }

    @Test
    void enumConstantsAreNotNull() {
        assertNotNull(OverridePolicy.ALLOW);
        assertNotNull(OverridePolicy.WARN);
        assertNotNull(OverridePolicy.REJECT);
    }

    @Test
    void toStringReturnsName() {
        assertEquals("ALLOW", OverridePolicy.ALLOW.toString());
        assertEquals("WARN", OverridePolicy.WARN.toString());
        assertEquals("REJECT", OverridePolicy.REJECT.toString());
    }

    @Test
    void nameReturnsName() {
        assertEquals("ALLOW", OverridePolicy.ALLOW.name());
        assertEquals("WARN", OverridePolicy.WARN.name());
        assertEquals("REJECT", OverridePolicy.REJECT.name());
    }

    @Test
    void ordinalIsCorrect() {
        assertEquals(0, OverridePolicy.ALLOW.ordinal());
        assertEquals(1, OverridePolicy.WARN.ordinal());
        assertEquals(2, OverridePolicy.REJECT.ordinal());
    }

    @Test
    void compareToWorks() {
        assertTrue(OverridePolicy.ALLOW.compareTo(OverridePolicy.WARN) < 0);
        assertTrue(OverridePolicy.WARN.compareTo(OverridePolicy.REJECT) < 0);
        assertTrue(OverridePolicy.ALLOW.compareTo(OverridePolicy.REJECT) < 0);

        assertTrue(OverridePolicy.REJECT.compareTo(OverridePolicy.WARN) > 0);
        assertTrue(OverridePolicy.WARN.compareTo(OverridePolicy.ALLOW) > 0);
        assertTrue(OverridePolicy.REJECT.compareTo(OverridePolicy.ALLOW) > 0);

        assertEquals(0, OverridePolicy.ALLOW.compareTo(OverridePolicy.ALLOW));
        assertEquals(0, OverridePolicy.WARN.compareTo(OverridePolicy.WARN));
        assertEquals(0, OverridePolicy.REJECT.compareTo(OverridePolicy.REJECT));
    }
}