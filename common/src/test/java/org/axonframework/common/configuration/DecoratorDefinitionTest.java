package org.axonframework.common.configuration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DecoratorDefinitionTest {

    @Test
    void forTypeCreatesDefinitionWithDecorator() {
        DecoratorDefinition<String, String> definition =
                DecoratorDefinition.forType(String.class)
                                  .with((config, name, delegate) -> delegate);

        assertTrue(definition instanceof DecoratorDefinition.CompletedDecoratorDefinition);
    }
}