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

package org.axonframework.common.configuration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentIdentifierTest {

    @Test
    void matchesReturnsTrueForSameTypeAndName() {
        Component.Identifier<Object> left = new Component.Identifier<>(Object.class, "name");
        Component.Identifier<String> right = new Component.Identifier<>(String.class, "name");

        assertTrue(left.matches(right));
    }

    @Test
    void matchesReturnsFalseForDifferentName() {
        Component.Identifier<String> left = new Component.Identifier<>(String.class, "left");
        Component.Identifier<String> right = new Component.Identifier<>(String.class, "right");

        assertFalse(left.matches(right));
    }
}
