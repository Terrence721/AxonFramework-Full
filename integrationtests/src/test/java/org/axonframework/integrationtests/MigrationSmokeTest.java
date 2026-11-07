package org.axonframework.integrationtests;

import org.axonframework.migration.UpgradeJava;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class MigrationSmokeTest {
    @Test
    void migrationModuleLoads() {
        assertNotNull(UpgradeJava.class);
    }
}
