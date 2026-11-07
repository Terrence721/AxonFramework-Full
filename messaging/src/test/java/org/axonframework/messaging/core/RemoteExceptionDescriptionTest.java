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

package org.axonframework.messaging.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RemoteExceptionDescriptionTest {
    @Test
    void describingProducesOneEntryForSimpleException() {
        RuntimeException exception = new RuntimeException("boom");
        RemoteExceptionDescription description = RemoteExceptionDescription.describing(exception);

        assertEquals(List.of("java.lang.RuntimeException: boom"), description.getDescriptions());
        assertFalse(description.isPersistent());
    }

    @Test
    void includesCausesInOrder() {
        RuntimeException cause = new RuntimeException("cause");
        RuntimeException outer = new RuntimeException("outer", cause);
        RemoteExceptionDescription description = RemoteExceptionDescription.describing(outer);

        assertEquals(2, description.getDescriptions().size());
    }
}
