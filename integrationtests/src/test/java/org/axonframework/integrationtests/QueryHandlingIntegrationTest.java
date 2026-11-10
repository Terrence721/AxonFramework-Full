package org.axonframework.integrationtests;

import java.util.ArrayList;
import java.util.List;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.QualifiedName;
import org.axonframework.messaging.core.EmptyApplicationContext;
import org.axonframework.messaging.core.unitofwork.SimpleUnitOfWorkFactory;
import org.axonframework.messaging.queryhandling.GenericQueryMessage;
import org.axonframework.messaging.queryhandling.GenericQueryResponseMessage;
import org.axonframework.messaging.queryhandling.QueryResponseMessage;
import org.axonframework.messaging.queryhandling.SimpleQueryBus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueryHandlingIntegrationTest {
    @Test
    void queriesReturnResponses() {
        SimpleQueryBus queryBus = new SimpleQueryBus(new SimpleUnitOfWorkFactory(EmptyApplicationContext.INSTANCE));
        MessageType queryType = new MessageType("example", "Query", MessageType.DEFAULT_VERSION);
        QualifiedName queryName = new QualifiedName("example", "Query");

        queryBus.subscribe(queryName, (query, context) ->
                MessageStream.just(new GenericQueryResponseMessage(queryType, "result")));

        List<QueryResponseMessage> responses = queryBus
                .query(new GenericQueryMessage(queryType, "payload"), null)
                .collect(ArrayList<QueryResponseMessage>::new, (list, response) -> list.add(response))
                .join();

        assertEquals(1, responses.size());
        assertEquals("result", responses.get(0).payload());
    }
}
