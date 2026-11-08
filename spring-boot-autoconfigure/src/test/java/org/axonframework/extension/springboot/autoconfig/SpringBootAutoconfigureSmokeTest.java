package org.axonframework.extension.springboot.autoconfig;

import org.axonframework.extension.springboot.*;
import org.axonframework.extension.springboot.util.DefaultEntityRegistrar;
import org.axonframework.extension.springboot.util.OnMissingQualifiedBeanCondition;
import org.axonframework.extension.springboot.util.OnQualifiedBeanCondition;
import org.axonframework.extension.springboot.util.jpa.ContainerManagedEntityManagerProvider;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SpringBootAutoconfigureSmokeTest {
    @Test
    void utilityAndPropertyClassesInstantiate() throws Exception {
        for (Class<?> type : List.of(
                OnQualifiedBeanCondition.class,
                OnMissingQualifiedBeanCondition.class,
                DefaultEntityRegistrar.class,
                ContainerManagedEntityManagerProvider.class,
                ConverterProperties.class,
                EventProcessorProperties.class,
                TimeoutProperties.class,
                TokenStoreProperties.class,
                TracingProperties.class,
                UpdateCheckerProperties.class)) {
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            assertNotNull(constructor.newInstance(), type.getName());
        }
    }

    @Test
    void axonAutoConfigurationLoads() throws Exception {
        assertNotNull(Class.forName("org.axonframework.extension.springboot.autoconfig.AxonAutoConfiguration"));
    }
}
