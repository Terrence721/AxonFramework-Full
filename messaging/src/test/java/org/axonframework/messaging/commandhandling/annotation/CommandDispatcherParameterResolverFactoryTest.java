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

import org.axonframework.common.infra.ComponentDescriptor;
import org.axonframework.messaging.commandhandling.gateway.CommandDispatcher;
import org.axonframework.messaging.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.commandhandling.gateway.CommandResult;
import org.axonframework.messaging.core.Metadata;
import org.axonframework.messaging.core.annotation.ParameterResolver;
import org.axonframework.messaging.core.unitofwork.ProcessingContext;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link CommandDispatcherParameterResolverFactory}.
 */
class CommandDispatcherParameterResolverFactoryTest {

    private final CommandDispatcherParameterResolverFactory factory = new CommandDispatcherParameterResolverFactory();

    @SuppressWarnings("unused")
    void handler(CommandDispatcher dispatcher, String other) {
    }

    private Method handlerMethod() throws NoSuchMethodException {
        return getClass().getDeclaredMethod("handler", CommandDispatcher.class, String.class);
    }

    @Test
    void createsAResolverForCommandDispatcherParameters() throws NoSuchMethodException {
        Method method = handlerMethod();

        assertNotNull(factory.createInstance(method, method.getParameters(), 0));
    }

    @Test
    void ignoresOtherParameterTypes() throws NoSuchMethodException {
        Method method = handlerMethod();

        assertNull(factory.createInstance(method, method.getParameters(), 1));
    }

    @Test
    void resolverAlwaysMatchesAndResolvesAContextBoundDispatcher() throws Exception {
        Method method = handlerMethod();
        List<Object> sent = new ArrayList<>();
        CommandGateway gateway = new CommandGateway() {
            @Override
            public CommandResult send(Object command, Metadata metadata, @Nullable ProcessingContext context) {
                sent.add(context);
                return null;
            }

            @Override
            public void describeTo(ComponentDescriptor descriptor) {
            }
        };
        ProcessingContext context = (ProcessingContext) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{ProcessingContext.class},
                (proxy, m, args) -> m.getName().equals("component") ? gateway : null
        );
        ParameterResolver<CommandDispatcher> resolver = factory.createInstance(method, method.getParameters(), 0);

        assertTrue(resolver.matches(context));
        CommandDispatcher dispatcher = resolver.resolveParameterValue(context).join();
        dispatcher.send("command");

        assertEquals(List.of(context), sent);
    }
}
