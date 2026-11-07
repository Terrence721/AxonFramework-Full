package org.axonframework.integrationtests;

import org.axonframework.messaging.core.Metadata;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetadataSmokeTest {
    @Test
    void metadataEmptyAcrossModules() {
        Metadata metadata = Metadata.emptyInstance();

        assertTrue(metadata.isEmpty());
        assertEquals(null, metadata.get("missing"));
    }
}
