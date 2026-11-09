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
import org.axonframework.conversion.Converter;
import org.axonframework.messaging.commandhandling.CommandMessage;
import org.axonframework.messaging.commandhandling.CommandResultMessage;
import org.axonframework.messaging.commandhandling.GenericCommandMessage;
import org.axonframework.messaging.commandhandling.NoHandlerForCommandException;
import org.axonframework.messaging.core.ClassBasedMessageTypeResolver;
import org.axonframework.messaging.core.EmptyApplicationContext;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.MessageTypeResolver;
import org.axonframework.messaging.core.QualifiedName;
import org.axonframework.messaging.core.annotation.AnnotatedMessageHandlingMemberDefinition;
import org.axonframework.messaging.core.annotation.DefaultParameterResolverFactory;
import org.axonframework.messaging.core.annotation.MultiHandlerDefinition;
import org.axonframework.messaging.core.conversion.DelegatingMessageConverter;
import org.axonframework.messaging.core.conversion.MessageConverter;
import org.axonframework.messaging.core.unitofwork.SimpleUnitOfWorkFactory;
import org.axonframework.messaging.core.unitofwork.UnitOfWorkFactory;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link AnnotatedCommandHandlingComponent} and the {@link MethodCommandHandlerDefinition}.
 */
class AnnotatedCommandHandlingComponentTest {

    private static final DefaultParameterResolverFactory PARAMETERS = new DefaultParameterResolverFactory();
    private static final MultiHandlerDefinition HANDLERS = MultiHandlerDefinition.ordered(
            new MethodCommandHandlerDefinition(), new AnnotatedMessageHandlingMemberDefinition()
    );

    private static final Converter IDENTITY_CONVERTER = new Converter() {
        @SuppressWarnings("unchecked")
        @Override
        public <T> @Nullable T convert(@Nullable Object input, Type targetType) {
            return (T) input;
        }

        @Override
        public void describeTo(ComponentDescriptor descriptor) {
        }
    };

    private final MessageTypeResolver typeResolver = new ClassBasedMessageTypeResolver();
    private final MessageConverter converter = new DelegatingMessageConverter(IDENTITY_CONVERTER);
    private final UnitOfWorkFactory unitOfWorkFactory = new SimpleUnitOfWorkFactory(EmptyApplicationContext.INSTANCE);

    static class Handlers {

        final AtomicReference<String> lastGreeting = new AtomicReference<>();

        @CommandHandler(commandName = "greet")
        String greet(String name) {
            lastGreeting.set(name);
            return "Hello " + name;
        }

        @CommandHandler(commandName = "boom")
        String boom(String ignored) {
            throw new IllegalStateException("kaboom");
        }

        @CommandHandler
        Integer lengthOf(Long payload) {
            return payload.intValue();
        }

        @CommandHandler(commandName = "nothing")
        void nothing(Integer ignored) {
        }
    }

    private AnnotatedCommandHandlingComponent<Handlers> component(Handlers handlers) {
        return new AnnotatedCommandHandlingComponent<>(handlers, PARAMETERS, HANDLERS, typeResolver, converter);
    }

    private MessageStream.Single<CommandResultMessage> handle(AnnotatedCommandHandlingComponent<Handlers> component,
                                                              String name,
                                                              Object payload) {
        CommandMessage command = new GenericCommandMessage(new MessageType(name), payload);
        return unitOfWorkFactory.create()
                                .executeWithResult(context -> CompletableFuture.completedFuture(
                                        component.handle(command, context)
                                ))
                                .join();
    }

    @Test
    void supportsCommandsByExplicitNameAndByResolvedPayloadType() {
        Set<QualifiedName> supported = component(new Handlers()).supportedCommands();

        assertEquals(
                Set.of(new QualifiedName("greet"),
                       new QualifiedName("boom"),
                       new QualifiedName("nothing"),
                       typeResolver.resolveOrThrow(Long.class).qualifiedName()),
                supported
        );
    }

    @Test
    void supportedCommandsAreACopy() {
        AnnotatedCommandHandlingComponent<Handlers> component = component(new Handlers());

        assertThrows(UnsupportedOperationException.class, () -> component.supportedCommands().clear());
    }

    @Test
    void constructorRejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> new AnnotatedCommandHandlingComponent<>(
                null, PARAMETERS, HANDLERS, typeResolver, converter));
        assertThrows(NullPointerException.class, () -> new AnnotatedCommandHandlingComponent<>(
                new Handlers(), PARAMETERS, HANDLERS, null, converter));
        assertThrows(NullPointerException.class, () -> new AnnotatedCommandHandlingComponent<>(
                new Handlers(), PARAMETERS, HANDLERS, typeResolver, null));
    }

    @Test
    void nullMessageTypeResolverIsReportedAsSuch() {
        NullPointerException exception = assertThrows(NullPointerException.class,
                                                      () -> new AnnotatedCommandHandlingComponent<>(
                                                              new Handlers(), PARAMETERS, HANDLERS, null, converter));

        assertTrue(exception.getMessage().contains("MessageTypeResolver"));
    }

    @Test
    void handlesACommandAndReturnsTheResultAsACommandResult() {
        Handlers handlers = new Handlers();

        CommandResultMessage result =
                handle(component(handlers), "greet", "Bob").asCompletableFuture().join().message();

        assertEquals("Hello Bob", result.payload());
        assertEquals("Bob", handlers.lastGreeting.get());
    }

    @Test
    void handlesACommandRegisteredByPayloadTypeUsingTheResolvedName() {
        String name = typeResolver.resolveOrThrow(Long.class).qualifiedName().fullName();

        CommandResultMessage result = handle(component(new Handlers()), name, 42L).asCompletableFuture().join().message();

        assertEquals(42, result.payload());
    }

    @Test
    void voidHandlerCompletesWithoutAFailure() {
        MessageStream.Single<CommandResultMessage> result = handle(component(new Handlers()), "nothing", 1);

        result.asCompletableFuture().join();

        assertTrue(result.error().isEmpty());
    }

    @Test
    void commandWithoutHandlerFails() {
        MessageStream.Single<CommandResultMessage> result = handle(component(new Handlers()), "unknown", "x");

        assertInstanceOf(NoHandlerForCommandException.class, result.error().orElseThrow());
    }

    @Test
    void handlerExceptionsFailTheResult() {
        MessageStream.Single<CommandResultMessage> result = handle(component(new Handlers()), "boom", "x");

        Throwable error = assertThrows(java.util.concurrent.CompletionException.class,
                                       () -> result.asCompletableFuture().join()).getCause();

        assertTrue(error instanceof IllegalStateException
                           || (error.getCause() instanceof IllegalStateException));
    }

    @Test
    void describesItself() {
        Handlers handlers = new Handlers();
        Map<String, Object> properties = new HashMap<>();
        java.util.List<Object> wrapped = new java.util.ArrayList<>();
        ComponentDescriptor descriptor = (ComponentDescriptor) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{ComponentDescriptor.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("describeProperty") && args.length == 2) {
                        properties.put((String) args[0], args[1]);
                    } else if (method.getName().equals("describeWrapperOf")) {
                        wrapped.add(args[0]);
                    }
                    return null;
                }
        );

        component(handlers).describeTo(descriptor);

        assertSame(handlers, properties.get("target"));
        assertSame(typeResolver, properties.get("messageTypeResolver"));
        assertSame(converter, properties.get("converter"));
        assertEquals(1, wrapped.size());
    }
}
