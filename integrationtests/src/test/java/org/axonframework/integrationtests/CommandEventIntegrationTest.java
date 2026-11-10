package org.axonframework.integrationtests;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.axonframework.messaging.commandhandling.GenericCommandMessage;
import org.axonframework.messaging.commandhandling.GenericCommandResultMessage;
import org.axonframework.messaging.commandhandling.SimpleCommandBus;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.QualifiedName;
import org.axonframework.messaging.core.unitofwork.SimpleUnitOfWorkFactory;
import org.axonframework.messaging.core.EmptyApplicationContext;
import org.axonframework.messaging.eventhandling.GenericEventMessage;
import org.axonframework.messaging.eventhandling.SimpleEventBus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandEventIntegrationTest {
    @Test
    void commandHandlingPublishesEvent() {
        SimpleEventBus eventBus = new SimpleEventBus();
        SimpleCommandBus commandBus = new SimpleCommandBus(new SimpleUnitOfWorkFactory(EmptyApplicationContext.INSTANCE));

        AtomicBoolean eventHandled = new AtomicBoolean(false);
        eventBus.subscribe((events, processingContext) -> {
            eventHandled.set(true);
            return CompletableFuture.completedFuture(null);
        });

        MessageType commandType = new MessageType("example", "Command", MessageType.DEFAULT_VERSION);
        QualifiedName commandName = new QualifiedName("example", "Command");
        commandBus.subscribe(commandName, (command, processingContext) -> {
            eventBus.publish(null, List.of(new GenericEventMessage(
                    new MessageType("example", "Event", MessageType.DEFAULT_VERSION), "payload", Map.of()))).join();
            return MessageStream.just(new GenericCommandResultMessage(commandType, (Object) null));
        });

        commandBus.dispatch(new GenericCommandMessage(commandType, "payload"), null).join();

        assertTrue(eventHandled.get());
    }
}
