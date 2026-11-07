package org.axonframework.integrationtests;

import org.axonframework.modelling.RepositoryAlreadyRegisteredException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ModellingSmokeTest {
    @Test
    void modellingModuleLoads() {
        assertNotNull(RepositoryAlreadyRegisteredException.class);
    }
}
