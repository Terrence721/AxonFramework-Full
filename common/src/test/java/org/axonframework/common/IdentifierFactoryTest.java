package org.axonframework.common;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class IdentifierFactoryTest {

    @Test
    void instanceIsAvailable() {
        assertNotNull(IdentifierFactory.getInstance());
    }

    @Test
    void defaultInstanceGeneratesUuid() {
        String identifier = IdentifierFactory.getInstance().generateIdentifier();

        assertNotNull(identifier);
        assertEquals(identifier, UUID.fromString(identifier).toString());
    }

    @Test
    void instanceIsStable() {
        assertSame(IdentifierFactory.getInstance(), IdentifierFactory.getInstance());
    }
}