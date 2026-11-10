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

package org.axonframework.modelling.configuration;

import org.axonframework.common.configuration.Configuration;
import org.axonframework.common.configuration.LifecycleRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntityModuleTest {

    @Test
    void entityNameConcatenatesTypeNames() {
        EntityModule<String, Object> module = new EntityModule<>() {
            @Override
            public String name() {
                return "entity-module";
            }

            @Override
            public Class<String> idType() {
                return String.class;
            }

            @Override
            public Class<Object> entityType() {
                return Object.class;
            }

            @Override
            public org.axonframework.common.configuration.Configuration build(Configuration parent,
                                                                          LifecycleRegistry lifecycleRegistry) {
                return parent;
            }
        };

        assertEquals("java.lang.Object#java.lang.String", module.entityName());
    }
}
