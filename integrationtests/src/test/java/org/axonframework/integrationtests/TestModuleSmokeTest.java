package org.axonframework.integrationtests;

import org.axonframework.test.fixture.AxonTestFixture;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class TestModuleSmokeTest {
    @Test
    void testModuleLoads() {
        assertNotNull(AxonTestFixture.class);
    }
}
