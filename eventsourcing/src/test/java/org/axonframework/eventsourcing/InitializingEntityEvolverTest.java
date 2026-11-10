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

package org.axonframework.eventsourcing;

import org.axonframework.modelling.EntityEvolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import org.axonframework.eventsourcing.handler.InitializingEntityEvolver;

class InitializingEntityEvolverTest {
    @Test
    void constructorRejectsNullFactory() {
        assertThrows(NullPointerException.class, () ->
                new InitializingEntityEvolver<>(null, (entity, message, context) -> entity));
    }

    @Test
    void initializeRejectsNullContext() {
        InitializingEntityEvolver<String, String> evolver =
            new InitializingEntityEvolver<>( (id, event, context) -> id, (EntityEvolver<String>) (entity, message, context) -> entity);

        assertThrows(NullPointerException.class, () -> evolver.initialize("id", null));
    }

    @Test
    void evolveRejectsNullContext() {
        InitializingEntityEvolver<String, String> evolver =
            new InitializingEntityEvolver<>( (id, event, context) -> id, (EntityEvolver<String>) (entity, message, context) -> entity);

        assertThrows(NullPointerException.class, () ->
                evolver.evolve("id", null, null, null));
    }

}
