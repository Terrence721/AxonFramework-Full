/*
 * Copyright (c) 2026. Terrence Daniels
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.axonframework.messaging.commandhandling.annotation;

import org.axonframework.common.configuration.AxonConfiguration;
import org.axonframework.messaging.commandhandling.gateway.CommandDispatcher;
import org.axonframework.messaging.core.annotation.ParameterResolverFactory;
import org.axonframework.messaging.core.configuration.MessagingConfigurer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test class validating the {@link CommandDispatcherParameterResolverFactoryConfigurationEnhancer}.
 */
class CommandDispatcherParameterResolverFactoryConfigurationEnhancerTest {

    @SuppressWarnings("java:S119")
    void handler(CommandDispatcher dispatcher, String other) {
    }

    @Test
    void enhancerRegistersAFactoryResolvingCommandDispatcherParameters() throws NoSuchMethodException {
        AxonConfiguration configuration = MessagingConfigurer.create()
                                                             .componentRegistry(
                                                                     registry -> new CommandDispatcherParameterResolverFactoryConfigurationEnhancer().enhance(
                                                                             registry))
                                                             .build();
        Method method = getClass().getDeclaredMethod("handler", CommandDispatcher.class, String.class);

        ParameterResolverFactory factory = configuration.getComponent(ParameterResolverFactory.class);

        assertNotNull(factory.createInstance(method, method.getParameters(), 0));
        assertNull(factory.createInstance(method, method.getParameters(), 1));
    }
}
