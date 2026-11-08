package org.axonframework.extension.springboot.test;

import org.axonframework.common.configuration.AxonConfiguration;
import org.axonframework.messaging.core.configuration.MessagingConfigurer;
import org.axonframework.test.fixture.AxonTestFixture;
import org.axonframework.test.fixture.MessagesRecordingConfigurationEnhancer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.env.StandardEnvironment;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SpringBootStarterTestSmokeTest {
    @Test
    void annotationAndConfigurationLoad() {
        assertNotNull(AxonSpringBootTest.class);
        AxonTestConfiguration configuration = new AxonTestConfiguration();
        assertNotNull(configuration.recordingEnhancer());
        assertNotNull(configuration);
    }

    @Test
    void fixtureCanBeBuiltFromConfiguration() throws Exception {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        AxonConfiguration axonConfiguration =
                MessagingConfigurer.create().componentRegistry(cr -> cr.registerEnhancer(new MessagesRecordingConfigurationEnhancer()))
                                      .start();
        AxonTestFixture fixture = new AxonTestConfiguration().axonTestFixture(
                axonConfiguration,
                beanFactory.getBeanProvider(AxonTestFixture.Customization.class),
                new StandardEnvironment()
        );

        assertNotNull(fixture);
    }
}
