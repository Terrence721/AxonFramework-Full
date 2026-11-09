package org.axonframework.common.configuration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComponentBuilderTest {

    @Test
    void buildsWithConfiguration() {
        Configuration configuration = null;
        ComponentBuilder<String> builder = config -> "built:" + (config == null);

        assertEquals("built:true", builder.build(configuration));
    }
}