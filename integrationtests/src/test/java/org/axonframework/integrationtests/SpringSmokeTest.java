package org.axonframework.integrationtests;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SpringSmokeTest {
    @Test
    void springModuleLoads() throws ClassNotFoundException {
        assertNotNull(Class.forName("org.axonframework.spring.config.BeanDefinitionUtils"));
    }
}
