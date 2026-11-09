package org.axonframework.common.configuration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModuleBuilderTest {

    @Test
    void buildsModule() {
        ModuleBuilder<Module> builder = () -> new Module() {
            @Override
            public String name() {
                return "module";
            }

            @Override
            public Configuration build(Configuration parent, LifecycleRegistry lifecycleRegistry) {
                return parent;
            }
        };

        assertEquals("module", builder.build().name());
    }
}