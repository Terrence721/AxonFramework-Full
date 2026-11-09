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
 * See the License for the specific language governing permissions and limitations
 * under the License.
 */

package org.axonframework.messaging.queryhandling;

import org.axonframework.common.AxonException;
import org.axonframework.common.AxonNonTransientException;
import org.axonframework.messaging.commandhandling.CommandHandler;
import org.axonframework.messaging.commandhandling.DuplicateCommandHandlerSubscriptionException;
import org.axonframework.messaging.core.HandlerExecutionException;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.QualifiedName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the exceptions of the query handling package, plus the equally worded duplicate subscription
 * exception of the command handling package.
 */
class QueryHandlingExceptionsTest {

    private static final QueryMessage QUERY = new GenericQueryMessage(new MessageType("test.query"), "payload");

    private static QueryHandler namedQueryHandler(String name) {
        return new QueryHandler() {
            @Override
            public org.axonframework.messaging.core.MessageStream<QueryResponseMessage> handle(
                    QueryMessage query, org.axonframework.messaging.core.unitofwork.ProcessingContext context) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }

    private static CommandHandler namedCommandHandler(String name) {
        return new CommandHandler() {
            @Override
            public org.axonframework.messaging.core.MessageStream.Single<
                    org.axonframework.messaging.commandhandling.CommandResultMessage> handle(
                    org.axonframework.messaging.commandhandling.CommandMessage command,
                    org.axonframework.messaging.core.unitofwork.ProcessingContext context) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }

    // --- CompletedEmitterException ---

    @Test
    void completedEmitterExceptionIsNonTransientAndKeepsItsMessage() {
        CompletedEmitterException exception = new CompletedEmitterException("emitter completed");

        assertEquals("emitter completed", exception.getMessage());
        assertInstanceOf(AxonNonTransientException.class, exception);
    }

    // --- DuplicateQueryHandlerSubscriptionException ---

    @Test
    void duplicateQueryHandlerMessageNamesTheNewHandlerAsTheRegistrationAndTheExistingAsThePreviousOne() {
        DuplicateQueryHandlerSubscriptionException exception = new DuplicateQueryHandlerSubscriptionException(
                new QualifiedName("test.query"), namedQueryHandler("existing"), namedQueryHandler("newcomer"));

        assertEquals("Duplicate subscription for query handler [test.query] detected. "
                             + "Registration of handler [newcomer] conflicts with previously registered handler "
                             + "[existing].", exception.getMessage());
    }

    @Test
    void duplicateQueryHandlerExceptionAlsoAcceptsAPlainMessage() {
        assertEquals("custom", new DuplicateQueryHandlerSubscriptionException("custom").getMessage());
    }

    @Test
    void duplicateCommandHandlerMessageNamesTheNewHandlerAsTheRegistrationAndTheExistingAsThePreviousOne() {
        DuplicateCommandHandlerSubscriptionException exception = new DuplicateCommandHandlerSubscriptionException(
                new QualifiedName("test.command"), namedCommandHandler("existing"), namedCommandHandler("newcomer"));

        assertEquals("Duplicate subscription for command [test.command] detected. "
                             + "Registration of handler [newcomer] conflicts with previously registered handler "
                             + "[existing].", exception.getMessage());
    }

    // --- NoHandlerForQueryException ---

    @Test
    void noHandlerForTheBusNamesTheQueryTypeAndExplainsHowHandlersAreMatched() {
        NoHandlerForQueryException exception = NoHandlerForQueryException.forBus(QUERY);

        assertTrue(exception.getMessage().startsWith(
                "No matching handler is available to handle query of type [" + QUERY.type() + "]."),
                   exception.getMessage());
        assertTrue(exception.getMessage().contains("the query handler's name should match the query's name"),
                   exception.getMessage());
        assertInstanceOf(AxonException.class, exception);
    }

    @Test
    void noHandlerForAHandlingComponentAlsoMentionsTheMethodParameters() {
        NoHandlerForQueryException exception = NoHandlerForQueryException.forHandlingComponent(QUERY);

        assertTrue(exception.getMessage().contains("[" + QUERY.type() + "]"), exception.getMessage());
        assertTrue(exception.getMessage().contains("all the parameters on the query handling method should be "
                                                           + "resolvable"), exception.getMessage());
    }

    @Test
    void noHandlerForQueryExceptionAlsoAcceptsAPlainMessage() {
        assertEquals("custom", new NoHandlerForQueryException("custom").getMessage());
    }

    // --- QueryExecutionException ---

    @Test
    void queryExecutionExceptionKeepsMessageAndCause() {
        RuntimeException cause = new RuntimeException("root");

        QueryExecutionException exception = new QueryExecutionException("boom", cause);

        assertEquals("boom", exception.getMessage());
        assertSame(cause, exception.getCause());
        assertInstanceOf(HandlerExecutionException.class, exception);
        assertTrue(HandlerExecutionException.resolveDetails(exception).isEmpty());
    }

    @Test
    void queryExecutionExceptionAcceptsANullCause() {
        assertNull(new QueryExecutionException("boom", null).getCause());
    }

    @Test
    void queryExecutionExceptionCarriesDetails() {
        QueryExecutionException exception =
                new QueryExecutionException("boom", new RuntimeException("root"), "the details");

        assertEquals("the details", exception.getDetails(String.class).orElseThrow());
    }

    @Test
    void queryExecutionExceptionCanBeCreatedWithoutAWritableStackTrace() {
        QueryExecutionException exception =
                new QueryExecutionException("boom", new RuntimeException("root"), "details", false);

        assertEquals(0, exception.getStackTrace().length);
        assertEquals("details", exception.getDetails(String.class).orElseThrow());
    }

    // --- SubscriptionQueryAlreadyRegisteredException ---

    @Test
    void subscriptionQueryAlreadyRegisteredNamesTheQueryIdentifier() {
        SubscriptionQueryAlreadyRegisteredException exception =
                new SubscriptionQueryAlreadyRegisteredException("query-1");

        assertEquals("There is already a subscription query with query identifier [query-1] present.",
                     exception.getMessage());
    }

    // --- QueryPriorityCalculator ---

    @Test
    void theDefaultPriorityCalculatorGivesEveryQueryPriorityZero() {
        QueryPriorityCalculator calculator = QueryPriorityCalculator.defaultCalculator();

        assertEquals(0, calculator.determinePriority(QUERY));
        assertEquals(0, calculator.determinePriority(new GenericQueryMessage(new MessageType("other"), "x")));
    }

    @Test
    void aCustomPriorityCalculatorIsAFunctionOfTheQuery() {
        QueryPriorityCalculator calculator = query -> query.payload().toString().length();

        assertEquals("payload".length(), calculator.determinePriority(QUERY));
    }
}
