package org.axonframework.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrationTest {

    @Test
    void canBeImplementedAsLambda() {
        Registration registration = () -> true;

        assertTrue(registration.cancel());
        Registration neverRegistered = () -> false;
        assertFalse(neverRegistered.cancel());
    }
}