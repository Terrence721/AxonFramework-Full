package org.axonframework.extension.spring;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.RootBeanDefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeanDefinitionUtilsTest {
    @Test
    void extractsPackageNameFromBeanClassName() {
        RootBeanDefinition definition = new RootBeanDefinition(String.class);

        assertEquals("java.lang", BeanDefinitionUtils.extractPackageName(definition));
    }

    @Test
    void defaultsPackageNameForMissingDots() {
        RootBeanDefinition definition = new RootBeanDefinition();
        definition.setBeanClassName("TopLevel");

        assertEquals("default", BeanDefinitionUtils.extractPackageName(definition));
    }

    @Test
    void resolvesClassNameFromAbstractBeanClass() {
        RootBeanDefinition definition = new RootBeanDefinition(String.class);

        assertTrue(BeanDefinitionUtils.resolveClassName(definition).isPresent());
        assertEquals("java.lang.String", BeanDefinitionUtils.resolveClassName(definition).get());
    }
}
