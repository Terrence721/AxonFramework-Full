package org.axonframework.common;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DefaultIdentifierFactoryTest {

    @Test
    void generateIdentifierReturnsUuidString() {
        String identifier = new DefaultIdentifierFactory().generateIdentifier();

        assertNotNull(identifier);
        assertEquals(identifier, UUID.fromString(identifier).toString());
    }
}