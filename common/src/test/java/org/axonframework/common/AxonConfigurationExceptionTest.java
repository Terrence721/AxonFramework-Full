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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AxonConfigurationExceptionTest {

    @Test
    void constructsMessageAndCause() {
        RuntimeException cause = new RuntimeException("root");
        AxonConfigurationException exception = new AxonConfigurationException("bad config", cause);

        assertEquals("bad config", exception.getMessage());
        assertTrue(exception.getCause() == cause);
    }

    @Test
    void isNonTransient() {
        AxonConfigurationException exception = new AxonConfigurationException("bad config");

        assertTrue(axonNonTransient(exception));
    }

    private boolean axonNonTransient(Throwable throwable) {
        return throwable instanceof org.axonframework.common.AxonNonTransientException;
    }
}
