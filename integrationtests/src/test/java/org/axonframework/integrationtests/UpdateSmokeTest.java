package org.axonframework.integrationtests;

import org.axonframework.update.api.Artifact;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class UpdateSmokeTest {
    @Test
    void updateModuleLoads() {
        assertNotNull(Artifact.class);
    }
}
