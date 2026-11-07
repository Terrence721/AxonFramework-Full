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

package org.axonframework.eventsourcing.eventstore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PositionTest {
    @Test
    void globalIndexPositionMinReturnsSelfWhenSmaller() {
        GlobalIndexPosition first = new GlobalIndexPosition(5);
        GlobalIndexPosition second = new GlobalIndexPosition(9);

        assertEquals(first, first.min(second));
    }

    @Test
    void globalIndexPositionMinReturnsOtherWhenSmaller() {
        GlobalIndexPosition first = new GlobalIndexPosition(9);
        GlobalIndexPosition second = new GlobalIndexPosition(5);

        assertEquals(second, first.min(second));
    }

    @Test
    void toIndexReturnsStoredIndex() {
        assertEquals(7L, GlobalIndexPosition.toIndex(new GlobalIndexPosition(7)));
    }
}
