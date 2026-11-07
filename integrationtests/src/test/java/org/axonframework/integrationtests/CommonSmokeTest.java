package org.axonframework.integrationtests;

import org.axonframework.common.Assert;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class CommonSmokeTest {
    @Test
    void commonUtilitiesLoad() {
        assertDoesNotThrow(() -> Assert.isTrue(true, () -> "ok"));
    }
}
