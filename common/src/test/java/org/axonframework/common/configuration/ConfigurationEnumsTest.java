/*
 * Copyright (c) 2026. Terrence Daniels
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test class validating the {@link OverridePolicy} and {@link SearchScope} enumerations.
 */
class ConfigurationEnumsTest {

    @Test
    void overridePolicyDeclaresAllowWarnAndRejectInThatOrder() {
        assertEquals(List.of(OverridePolicy.ALLOW, OverridePolicy.WARN, OverridePolicy.REJECT),
                     List.of(OverridePolicy.values()));
    }

    @Test
    void searchScopeDeclaresAllCurrentAndAncestorsInThatOrder() {
        assertEquals(List.of(SearchScope.ALL, SearchScope.CURRENT, SearchScope.ANCESTORS),
                     List.of(SearchScope.values()));
    }

    @Test
    void enumConstantsCanBeLookedUpByName() {
        assertEquals(OverridePolicy.REJECT, OverridePolicy.valueOf("REJECT"));
        assertEquals(SearchScope.ANCESTORS, SearchScope.valueOf("ANCESTORS"));
    }

    @Test
    void unknownEnumNamesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> OverridePolicy.valueOf("IGNORE"));
        assertThrows(IllegalArgumentException.class, () -> SearchScope.valueOf("PARENT"));
    }
}
