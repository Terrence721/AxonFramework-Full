package org.axonframework.integrationtests;

import org.axonframework.extension.spring.BeanDefinitionUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SpringSmokeTest {
    @Test
    void springModuleLoads() {
        assertNotNull(BeanDefinitionUtils.class);
    }
}
