package org.axonframework.common.configuration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecoratorDefinitionsTest {

    @Test
    void registerAndDefinitions() {
        DecoratorDefinitions definitions = new DecoratorDefinitions();
        @SuppressWarnings("unchecked")
        DecoratorDefinition.CompletedDecoratorDefinition<String, String> completed =
                (DecoratorDefinition.CompletedDecoratorDefinition<String, String>) (DecoratorDefinition<?, ?>)
                        DecoratorDefinition.forType(String.class)
                                           .with((config, name, delegate) -> delegate);
        definitions.register(completed);

        assertEquals(1, definitions.definitions().size());
    }

    @Test
    void applyToWrapsMatchingComponent() {
        Components components = new Components();
        Component.Identifier<String> identifier = new Component.Identifier<>(String.class, "cmp");
        components.put(new InstantiatedComponentDefinition<>(identifier, "base"));
        DecoratorDefinitions definitions = new DecoratorDefinitions();
        @SuppressWarnings("unchecked")
        DecoratorDefinition.CompletedDecoratorDefinition<String, String> completed =
                (DecoratorDefinition.CompletedDecoratorDefinition<String, String>) (DecoratorDefinition<?, ?>)
                        DecoratorDefinition.forType(String.class)
                                           .with((config, name, delegate) -> delegate);
        definitions.register(completed);

        definitions.applyTo(components);

        assertTrue(components.get(identifier).isPresent());
        assertTrue(components.get(identifier).get() instanceof DecoratedComponent);
    }
}