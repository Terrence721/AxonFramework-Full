package org.axonframework.common.configuration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComponentDecoratorTest {

    @Test
    void lambdaDecoratorReturnsDelegate() {
        ComponentDecorator<String, String> decorator = (config, name, delegate) -> delegate + ":" + name;

        assertEquals("delegate:cmp", decorator.decorate(null, "cmp", "delegate"));
    }
}