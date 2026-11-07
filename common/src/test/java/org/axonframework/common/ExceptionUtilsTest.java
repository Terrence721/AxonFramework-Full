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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExceptionUtilsTest {

    @Test
    void findExceptionByClassReturnsMatchingCause() {
        RuntimeException cause = new RuntimeException("cause");
        RuntimeException outer = new RuntimeException("outer", cause);

        Optional<RuntimeException> result = ExceptionUtils.findException(outer, RuntimeException.class);

        assertTrue(result.isPresent());
        assertEquals("outer", result.get().getMessage());
    }

    @Test
    void isExplicitlyNonTransientReturnsTrueForWrappedException() {
        AxonConfigurationException root = new AxonConfigurationException("root");
        RuntimeException outer = new RuntimeException("outer", root);

        assertTrue(ExceptionUtils.isExplicitlyNonTransient(outer));
    }

    @Test
    void isExplicitlyNonTransientReturnsFalseForNull() {
        assertFalse(ExceptionUtils.isExplicitlyNonTransient(null));
    }
}
