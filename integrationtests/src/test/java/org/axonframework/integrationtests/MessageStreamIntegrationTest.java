package org.axonframework.integrationtests;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.axonframework.messaging.core.GenericMessage;
import org.axonframework.messaging.core.Message;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.MessageType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

class MessageStreamIntegrationTest {
    @Test
    void streamsCanTransformAndCollectMessages() {
        Message first = new GenericMessage(new MessageType("example", "Message", MessageType.DEFAULT_VERSION), "one");
        Message second = new GenericMessage(new MessageType("example", "Message", MessageType.DEFAULT_VERSION), "two");

        CompletableFuture<List<String>> collected = MessageStream.fromItems(first, second).collect(
                java.util.ArrayList::new,
                (list, message) -> list.add(String.valueOf(message.payload()))
        );

        Assertions.assertEquals(List.of("one", "two"), collected.join());
    }
}
