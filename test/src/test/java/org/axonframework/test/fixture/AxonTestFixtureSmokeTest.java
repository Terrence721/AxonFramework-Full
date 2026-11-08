package org.axonframework.test.fixture;

import org.axonframework.messaging.core.configuration.MessagingConfigurer;
import org.axonframework.test.matchers.Matchers;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AxonTestFixtureSmokeTest {
    @Test
    void fixtureCanBeCreatedFromMessagingConfig() {
        AxonTestFixture fixture = AxonTestFixture.with(MessagingConfigurer.create());
        assertNotNull(fixture);
    }

    @Test
    void matchersProvideNoOpMatchers() {
        assertEquals("no events", Matchers.noEvents().toString());
    }
}
