package org.axonframework.integrationtests;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.axonframework.messaging.commandhandling.CommandResultMessage;
import org.axonframework.messaging.commandhandling.GenericCommandMessage;
import org.axonframework.messaging.commandhandling.GenericCommandResultMessage;
import org.axonframework.messaging.commandhandling.SimpleCommandBus;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.Metadata;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.QualifiedName;
import org.axonframework.messaging.core.EmptyApplicationContext;
import org.axonframework.messaging.core.unitofwork.SimpleUnitOfWorkFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetadataPropagationIntegrationTest {
    @Test
    void commandMetadataPropagatesToHandlerAndResult() {
        SimpleCommandBus commandBus = new SimpleCommandBus(new SimpleUnitOfWorkFactory(EmptyApplicationContext.INSTANCE));
        MessageType commandType = new MessageType("example", "Command", MessageType.DEFAULT_VERSION);
        QualifiedName commandName = new QualifiedName("example", "Command");
        AtomicReference<Metadata> capturedMetadata = new AtomicReference<>();

        commandBus.subscribe(commandName, (command, context) -> {
            capturedMetadata.set(command.metadata());
            return MessageStream.just(new GenericCommandResultMessage(commandType, command.metadata().get("key")));
        });

        CommandResultMessage result = commandBus.dispatch(
                new GenericCommandMessage(commandType, "payload", Map.of("key", "value")),
                null
        ).join();

        assertEquals("value", capturedMetadata.get().get("key"));
        assertEquals("value", result.payload());
    }
}
