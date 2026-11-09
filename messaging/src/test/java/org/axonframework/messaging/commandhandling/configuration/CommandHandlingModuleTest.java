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
package org.axonframework.messaging.commandhandling.configuration;

import org.axonframework.common.configuration.AxonConfiguration;
import org.axonframework.common.infra.ComponentDescriptor;
import org.axonframework.messaging.commandhandling.CommandBus;
import org.axonframework.messaging.commandhandling.CommandHandler;
import org.axonframework.messaging.commandhandling.CommandHandlingComponent;
import org.axonframework.messaging.commandhandling.CommandMessage;
import org.axonframework.messaging.commandhandling.CommandResultMessage;
import org.axonframework.messaging.commandhandling.GenericCommandMessage;
import org.axonframework.messaging.commandhandling.GenericCommandResultMessage;
import org.axonframework.messaging.commandhandling.NoHandlerForCommandException;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.QualifiedName;
import org.axonframework.messaging.core.configuration.MessagingConfigurer;
import org.axonframework.messaging.core.unitofwork.ProcessingContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test class validating the {@link CommandHandlingModule} and its {@link SimpleCommandHandlingModule}
 * implementation.
 */
class CommandHandlingModuleTest {

    private static final MessageType TYPE = new MessageType("test.command");
    private static final MessageType RESULT_TYPE = new MessageType("test.result");

    private AxonConfiguration configuration;

    @AfterEach
    void tearDown() {
        if (configuration != null) {
            configuration.shutdown();
        }
    }

    private CommandBus start(CommandHandlingModule.CommandHandlerPhase module) {
        configuration = MessagingConfigurer.create()
                                           .registerCommandHandlingModule(module)
                                           .build();
        configuration.start();
        return configuration.getComponent(CommandBus.class);
    }

    private static CommandMessage command() {
        return new GenericCommandMessage(TYPE, "payload");
    }

    private static MessageStream.Single<CommandResultMessage> result(
            String payload
    ) {
        return MessageStream.just(new GenericCommandResultMessage(RESULT_TYPE, payload));
    }

    @Test
    void namedRejectsANullModuleName() {
        assertThrows(NullPointerException.class, () -> CommandHandlingModule.named(null));
    }

    @Test
    void phasesRejectNullArguments() {
        CommandHandlingModule.CommandHandlerPhase phase = CommandHandlingModule.named("module").commandHandlers();

        assertThrows(NullPointerException.class,
                     () -> phase.commandHandler(null, (command, context) -> result("x")));
        assertThrows(NullPointerException.class,
                     () -> phase.commandHandler(TYPE.qualifiedName(),
                                                (org.axonframework.common.configuration.ComponentBuilder<
                                                        org.axonframework.messaging.commandhandling.CommandHandler>) null));
        assertThrows(NullPointerException.class,
                     () -> phase.commandHandler(TYPE.qualifiedName(),
                                                (CommandHandler) null));
        assertThrows(NullPointerException.class, () -> phase.commandHandlingComponent(null));
        NullPointerException interceptorFailure =
                assertThrows(NullPointerException.class, () -> phase.intercepted(null));
        assertEquals("The interceptor builder cannot be null.", interceptorFailure.getMessage());
        assertThrows(NullPointerException.class, () -> phase.autodetectedCommandHandlingComponent(null));
        assertThrows(NullPointerException.class, () -> phase.withExceptionHandler(null));
        assertThrows(NullPointerException.class,
                     () -> CommandHandlingModule.named("module").commandHandlers(null));
    }

    @Test
    void commandHandlersLambdaIsAppliedImmediately() {
        List<String> applied = new ArrayList<>();

        CommandHandlingModule.named("module").commandHandlers(phase -> applied.add("applied"));

        assertEquals(List.of("applied"), applied);
    }

    @Test
    void registeredCommandHandlerIsSubscribedWithTheCommandBus() {
        CommandBus bus = start(CommandHandlingModule.named("module")
                                                    .commandHandlers()
                                                    .commandHandler(TYPE.qualifiedName(),
                                                                    (command, context) -> result("handled")));

        assertEquals("handled", bus.dispatch(command(), null).join().payload());
    }

    @Test
    void registeredHandlingComponentIsSubscribedWithTheCommandBus() {
        CommandBus bus = start(CommandHandlingModule.named("module")
                                                    .commandHandlers()
                                                    .commandHandlingComponent(c -> new StubComponent("component")));

        assertEquals("component", bus.dispatch(command(), null).join().payload());
    }

    @Test
    void unregisteredCommandsHaveNoHandler() {
        CommandBus bus = start(CommandHandlingModule.named("module")
                                                    .commandHandlers()
                                                    .commandHandler(new QualifiedName("other"),
                                                                    (command, context) -> result("x")));

        CompletionException failure = assertThrows(CompletionException.class,
                                                   () -> bus.dispatch(command(), null).join());

        assertInstanceOf(NoHandlerForCommandException.class, failure.getCause());
    }

    @Test
    void interceptorsAreAppliedInRegistrationOrder() {
        List<String> invocations = new ArrayList<>();
        CommandBus bus = start(
                CommandHandlingModule.named("module")
                                     .commandHandlers()
                                     .commandHandler(TYPE.qualifiedName(), (command, context) -> {
                                         invocations.add("handler");
                                         return result("handled");
                                     })
                                     .intercepted(c -> (message, context, chain) -> {
                                         invocations.add("first");
                                         return chain.proceed(message, context);
                                     })
                                     .intercepted(c -> (message, context, chain) -> {
                                         invocations.add("second");
                                         return chain.proceed(message, context);
                                     })
        );

        bus.dispatch(command(), null).join();

        assertEquals(List.of("first", "second", "handler"), invocations);
    }

    @Test
    void exceptionHandlerCanSubstituteAResult() {
        CommandBus bus = start(
                CommandHandlingModule.named("module")
                                     .commandHandlers()
                                     .commandHandler(TYPE.qualifiedName(), (command, context) -> {
                                         throw new IllegalStateException("boom");
                                     })
                                     .withExceptionHandler(c -> (message, context, error) -> result("recovered"))
        );

        assertEquals("recovered", bus.dispatch(command(), null).join().payload());
    }

    @Test
    void exceptionHandlerFailureIsPropagated() {
        IllegalArgumentException replacement = new IllegalArgumentException("replacement");
        CommandBus bus = start(
                CommandHandlingModule.named("module")
                                     .commandHandlers()
                                     .commandHandler(TYPE.qualifiedName(), (command, context) -> {
                                         throw new IllegalStateException("boom");
                                     })
                                     .withExceptionHandler(c -> (message, context, error) -> {
                                         throw replacement;
                                     })
        );

        CompletionException failure = assertThrows(CompletionException.class,
                                                   () -> bus.dispatch(command(), null).join());

        assertSame(replacement, failure.getCause());
    }

    private static class StubComponent implements CommandHandlingComponent {

        private final String payload;

        private StubComponent(String payload) {
            this.payload = payload;
        }

        @Override
        public MessageStream.Single<CommandResultMessage> handle(
                CommandMessage command, ProcessingContext context) {
            return result(payload);
        }

        @Override
        public Set<QualifiedName> supportedCommands() {
            return Set.of(TYPE.qualifiedName());
        }

        @Override
        public void describeTo(ComponentDescriptor descriptor) {
        }
    }
}
