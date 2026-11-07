package org.axonframework.integrationtests;

import org.axonframework.messaging.commandhandling.GenericCommandMessage;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.QualifiedName;
import org.axonframework.messaging.core.EmptyApplicationContext;
import org.axonframework.messaging.core.unitofwork.SimpleUnitOfWorkFactory;
import org.axonframework.messaging.queryhandling.GenericQueryMessage;
import org.axonframework.messaging.queryhandling.QueryHandler;
import org.axonframework.messaging.queryhandling.QueryResponseMessage;
import org.axonframework.messaging.queryhandling.SimpleQueryBus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class QueryHandlingSmokeTest {
    @Test
    void queriesCanBeDispatched() {
        SimpleQueryBus queryBus = new SimpleQueryBus(new SimpleUnitOfWorkFactory(EmptyApplicationContext.INSTANCE));
        MessageType queryType = new MessageType("example", "Query", MessageType.DEFAULT_VERSION);
        QualifiedName queryName = new QualifiedName("example", "Query");

        queryBus.subscribe(queryName, (query, context) -> MessageStream.empty());

        MessageStream<QueryResponseMessage> result = queryBus.query(new GenericQueryMessage(queryType, "payload"), null);

        assertNotNull(result);
    }
}
