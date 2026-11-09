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

package org.axonframework.common.lifecycle;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link Phase} constants. Their relative order is part of the startup/shutdown contract, as
 * handlers are started in ascending and shut down in descending phase order.
 */
class PhaseTest {

    @Test
    void externalConnectionsStartBeforeEverythingElse() {
        assertTrue(Phase.EXTERNAL_CONNECTIONS < Phase.OUTBOUND_EVENT_CONNECTORS);
    }

    @Test
    void outboundConnectorsStartBeforeLocalHandlersAndInboundConnectors() {
        assertTrue(Phase.OUTBOUND_EVENT_CONNECTORS < Phase.LOCAL_MESSAGE_HANDLER_REGISTRATIONS);
        assertTrue(Phase.LOCAL_MESSAGE_HANDLER_REGISTRATIONS < Phase.INBOUND_COMMAND_CONNECTOR);
        assertTrue(Phase.INBOUND_COMMAND_CONNECTOR < Phase.INBOUND_EVENT_CONNECTORS);
    }

    @Test
    void commandAndQueryConnectorsShareTheirPhases() {
        assertEquals(Phase.LOCAL_MESSAGE_HANDLER_REGISTRATIONS, Phase.OUTBOUND_COMMAND_CONNECTORS);
        assertEquals(Phase.OUTBOUND_COMMAND_CONNECTORS, Phase.OUTBOUND_QUERY_CONNECTORS);
        assertEquals(Phase.INBOUND_COMMAND_CONNECTOR, Phase.INBOUND_QUERY_CONNECTOR);
    }

    @Test
    void instructionComponentsStartAfterInboundEventConnectorsWithoutOverflow() {
        assertEquals(Phase.INBOUND_EVENT_CONNECTORS + 10, Phase.INSTRUCTION_COMPONENTS);
        assertTrue(Phase.INSTRUCTION_COMPONENTS > Phase.INBOUND_EVENT_CONNECTORS);
    }

    @Test
    void isNonInstantiableUtilityClass() throws NoSuchMethodException {
        Constructor<Phase> constructor = Phase.class.getDeclaredConstructor();

        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        assertTrue(Modifier.isFinal(Phase.class.getModifiers()));
    }
}
