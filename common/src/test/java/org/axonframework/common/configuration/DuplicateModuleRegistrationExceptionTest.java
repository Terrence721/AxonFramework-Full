package org.axonframework.common.configuration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DuplicateModuleRegistrationExceptionTest {

    @Test
    void constructsMessageFromModuleName() {
        Module module = new Module() {
            @Override
            public String name() {
                return "duplicate-mod";
            }

            @Override
            public Configuration build(Configuration parent, LifecycleRegistry lifecycleRegistry) {
                return parent;
            }
        };

        DuplicateModuleRegistrationException exception = new DuplicateModuleRegistrationException(module);

        assertTrue(exception.getMessage().contains("duplicate-mod"));
    }
}