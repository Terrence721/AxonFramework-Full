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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.axonframework.messaging.commandhandling;

import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.Metadata;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link MetadataRoutingStrategy} and the {@link UnresolvedRoutingKeyPolicy}.
 */
class RoutingStrategiesTest {

    private static final MessageType TYPE = new MessageType("test.command");

    private static CommandMessage command(Metadata metadata) {
        return new GenericCommandMessage(TYPE, "payload", metadata);
    }

    @Test
    void metadataRoutingStrategyReturnsTheMetadataValue() {
        MetadataRoutingStrategy strategy = new MetadataRoutingStrategy("aggregate");

        assertEquals("abc", strategy.getRoutingKey(command(Metadata.with("aggregate", "abc"))));
    }

    @Test
    void metadataRoutingStrategyReturnsNullWhenTheKeyIsAbsent() {
        assertNull(new MetadataRoutingStrategy("aggregate").getRoutingKey(command(Metadata.with("other", "x"))));
    }

    @Test
    void metadataRoutingStrategyRejectsANullKey() {
        assertThrows(NullPointerException.class, () -> new MetadataRoutingStrategy(null));
    }

    @Test
    void errorPolicyThrowsACommandDispatchExceptionNamingTheCommandType() {
        CommandDispatchException exception = assertThrows(
                CommandDispatchException.class,
                () -> UnresolvedRoutingKeyPolicy.ERROR.getRoutingKey(command(Metadata.emptyInstance()))
        );

        assertTrue(exception.getMessage().contains("test.command"));
    }

    @Test
    void staticKeyPolicyAlwaysReturnsTheSameKey() {
        CommandMessage command = command(Metadata.emptyInstance());

        assertEquals("unresolved", UnresolvedRoutingKeyPolicy.STATIC_KEY.getRoutingKey(command));
        assertEquals("unresolved", UnresolvedRoutingKeyPolicy.STATIC_KEY.getRoutingKey(command));
    }

    @Test
    void randomKeyPolicyReturnsANonEmptyKeyThatDiffersBetweenCalls() {
        CommandMessage command = command(Metadata.emptyInstance());

        String first = UnresolvedRoutingKeyPolicy.RANDOM_KEY.getRoutingKey(command);
        String second = UnresolvedRoutingKeyPolicy.RANDOM_KEY.getRoutingKey(command);

        assertTrue(!first.isEmpty());
        assertNotEquals(first, second);
    }
}
