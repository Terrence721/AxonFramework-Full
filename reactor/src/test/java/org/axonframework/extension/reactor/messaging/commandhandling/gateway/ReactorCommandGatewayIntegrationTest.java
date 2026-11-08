package org.axonframework.extension.reactor.messaging.commandhandling.gateway;

import org.axonframework.common.infra.ComponentDescriptor;
import org.axonframework.messaging.commandhandling.CommandBus;
import org.axonframework.messaging.commandhandling.CommandMessage;
import org.axonframework.messaging.commandhandling.CommandResultMessage;
import org.axonframework.messaging.commandhandling.gateway.FutureCommandResult;
import org.axonframework.messaging.commandhandling.GenericCommandMessage;
import org.axonframework.messaging.commandhandling.GenericCommandResultMessage;
import org.axonframework.messaging.commandhandling.SimpleCommandBus;
import org.axonframework.messaging.core.ClassBasedMessageTypeResolver;
import org.axonframework.messaging.core.Message;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.Metadata;
import org.axonframework.messaging.core.QualifiedName;
import org.axonframework.messaging.core.EmptyApplicationContext;
import org.axonframework.messaging.core.unitofwork.ProcessingContext;
import org.axonframework.messaging.core.unitofwork.SimpleUnitOfWorkFactory;
import org.axonframework.messaging.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.commandhandling.gateway.CommandResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ReactorCommandGatewayIntegrationTest {
    @Test
    void dispatchesThroughReactorGateway() {
        SimpleCommandBus commandBus = new SimpleCommandBus(new SimpleUnitOfWorkFactory(EmptyApplicationContext.INSTANCE));
        MessageType commandType = new MessageType("example", "Command", MessageType.DEFAULT_VERSION);
        QualifiedName commandName = new QualifiedName("example", "Command");

        commandBus.subscribe(commandName, (command, context) ->
                MessageStream.just(new GenericCommandResultMessage(commandType, "ok")));

        CommandGateway delegate = new CommandGateway() {
            @Override
            public CommandResult send(Object command, Metadata metadata, ProcessingContext context) {
                CommandMessage commandMessage;
                if (command instanceof CommandMessage cm) {
                    commandMessage = cm;
                } else if (command instanceof Message message) {
                    commandMessage = new GenericCommandMessage(message.type(), message.payload(), message.metadata());
                } else {
                    commandMessage = new GenericCommandMessage(
                            new ClassBasedMessageTypeResolver().resolve(command.getClass()).orElseThrow(),
                            command, metadata);
                }
                return new FutureCommandResult(commandBus.dispatch(commandMessage, context));
            }

            @Override
            public void describeTo(ComponentDescriptor descriptor) {
            }
        };

        DefaultReactorCommandGateway gateway =
                new DefaultReactorCommandGateway(delegate, new ClassBasedMessageTypeResolver());

        assertEquals("ok", gateway.send(new GenericCommandMessage(commandType, "payload"), String.class).block());
        assertNotNull(gateway);
    }
}
