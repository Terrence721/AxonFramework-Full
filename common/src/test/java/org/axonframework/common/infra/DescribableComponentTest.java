package org.axonframework.common.infra;

import org.axonframework.common.configuration.Component;
import org.axonframework.common.configuration.InstantiatedComponentDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DescribableComponentTest {

    @Test
    void resolvedTypeNameUsesIdentifierTypeForComponents() {
        Component<String> component = new InstantiatedComponentDefinition<>(
                new Component.Identifier<>(String.class, "cmp"), "value");

        assertEquals(String.class.getName(), DescribableComponent.resolvedTypeName(component));
    }

    @Test
    void resolvedTypeNameUsesRuntimeClassOtherwise() {
        DescribableComponent component = descriptor -> {
        };

        assertEquals(component.getClass().getName(), DescribableComponent.resolvedTypeName(component));
    }
}