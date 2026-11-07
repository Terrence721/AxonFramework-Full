package org.axonframework.integrationtests;

import org.axonframework.extension.reactor.messaging.core.ReactorMessageDispatchInterceptor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ReactorSmokeTest {
    @Test
    void reactorModuleLoads() {
        assertNotNull(ReactorMessageDispatchInterceptor.class);
    }
}
