package org.axonframework.integrationtests;

import org.axonframework.messaging.commandhandling.CommandResultMessage;
import org.axonframework.messaging.commandhandling.GenericCommandMessage;
import org.axonframework.messaging.commandhandling.SimpleCommandBus;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.EmptyApplicationContext;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.QualifiedName;
import org.axonframework.messaging.commandhandling.GenericCommandResultMessage;
import org.axonframework.messaging.core.unitofwork.SimpleUnitOfWorkFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CommandHandlingSmokeTest {
    @Test
    void commandsCanBeDispatched() {
        SimpleCommandBus commandBus = new SimpleCommandBus(new SimpleUnitOfWorkFactory(EmptyApplicationContext.INSTANCE));
        QualifiedName commandName = new QualifiedName("example", "Command");
        MessageType commandType = new MessageType("example", "Command", MessageType.DEFAULT_VERSION);

        commandBus.subscribe(commandName, (command, context) -> MessageStream.just(new GenericCommandResultMessage(commandType, "ok")));

        CommandResultMessage result = commandBus.dispatch(new GenericCommandMessage(commandType, "payload"), null)
                .join();

        assertNotNull(result);
        assertEquals("ok", result.payload());
    }
}
