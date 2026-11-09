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
package org.axonframework.messaging.commandhandling.interception;

import org.axonframework.messaging.commandhandling.CommandMessage;
import org.axonframework.messaging.commandhandling.GenericCommandMessage;
import org.axonframework.messaging.commandhandling.GenericCommandResultMessage;
import org.axonframework.messaging.core.EmptyApplicationContext;
import org.axonframework.messaging.core.MessageHandlerInterceptorChain;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.Metadata;
import org.axonframework.messaging.core.sequencing.MetadataSequencingPolicy;
import org.axonframework.messaging.core.unitofwork.SimpleUnitOfWorkFactory;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test class validating the {@link CommandSequencingInterceptor}.
 */
class CommandSequencingInterceptorTest {

    private static final MessageType TYPE = new MessageType("test.command");

    private final SimpleUnitOfWorkFactory unitOfWorkFactory = new SimpleUnitOfWorkFactory(EmptyApplicationContext.INSTANCE);
    private final CommandSequencingInterceptor<CommandMessage> interceptor =
            new CommandSequencingInterceptor<>(new MetadataSequencingPolicy("aggregate"));
    private final List<String> processed = new ArrayList<>();

    private static CommandMessage command(String aggregate) {
        Metadata metadata = aggregate == null ? Metadata.emptyInstance() : Metadata.with("aggregate", aggregate);
        return new GenericCommandMessage(TYPE, "payload", metadata);
    }

    private MessageHandlerInterceptorChain<CommandMessage> chain(String name) {
        return (message, context) -> {
            processed.add(name);
            return MessageStream.just(new GenericCommandResultMessage(new MessageType("test.result"), name));
        };
    }

    /**
     * Starts a unit of work that intercepts the given {@code command} and only finishes once {@code gate} completes.
     */
    private CompletableFuture<Object> start(CommandMessage command, String name, CompletableFuture<Void> gate) {
        return unitOfWorkFactory.create().executeWithResult(context -> {
            interceptor.interceptOnHandle(command, context, chain(name));
            return gate.thenApply(ignored -> (Object) name);
        });
    }

    @Test
    void constructorRejectsANullPolicy() {
        assertThrows(NullPointerException.class, () -> new CommandSequencingInterceptor<>(null));
    }

    @Test
    void commandsWithoutASequenceIdentifierProceedImmediately() {
        CompletableFuture<Void> gate = new CompletableFuture<>();

        start(command(null), "first", gate);
        start(command(null), "second", gate);

        assertEquals(List.of("first", "second"), processed);
        gate.complete(null);
    }

    @Test
    void commandsWithDifferentSequenceIdentifiersProceedConcurrently() {
        CompletableFuture<Void> gate = new CompletableFuture<>();

        start(command("a"), "first", gate);
        start(command("b"), "second", gate);

        assertEquals(List.of("first", "second"), processed);
        gate.complete(null);
    }

    @Test
    void aCommandWaitsForThePreviousCommandWithTheSameSequenceIdentifier() {
        CompletableFuture<Void> firstGate = new CompletableFuture<>();
        CompletableFuture<Void> secondGate = new CompletableFuture<>();

        CompletableFuture<Object> first = start(command("a"), "first", firstGate);
        CompletableFuture<Object> second = start(command("a"), "second", secondGate);

        assertEquals(List.of("first"), processed);

        firstGate.complete(null);
        first.join();

        assertEquals(List.of("first", "second"), processed);
        secondGate.complete(null);
        second.join();
    }

    @Test
    void sequencedCommandsRunInTheOrderTheyArrived() {
        CompletableFuture<Void> firstGate = new CompletableFuture<>();
        CompletableFuture<Void> secondGate = new CompletableFuture<>();
        CompletableFuture<Void> thirdGate = new CompletableFuture<>();

        CompletableFuture<Object> first = start(command("a"), "first", firstGate);
        CompletableFuture<Object> second = start(command("a"), "second", secondGate);
        CompletableFuture<Object> third = start(command("a"), "third", thirdGate);

        firstGate.complete(null);
        first.join();
        assertEquals(List.of("first", "second"), processed);

        secondGate.complete(null);
        second.join();
        assertEquals(List.of("first", "second", "third"), processed);

        thirdGate.complete(null);
        third.join();
    }

    @Test
    void aFailedCommandStillReleasesTheNextOne() {
        CompletableFuture<Void> firstGate = new CompletableFuture<>();
        CompletableFuture<Void> secondGate = new CompletableFuture<>();

        CompletableFuture<Object> first = start(command("a"), "first", firstGate);
        CompletableFuture<Object> second = start(command("a"), "second", secondGate);
        firstGate.completeExceptionally(new IllegalStateException("boom"));
        assertThrows(Exception.class, first::join);

        assertEquals(List.of("first", "second"), processed);
        secondGate.complete(null);
        second.join();
    }

    @Test
    void theIdentifierIsFreedOnceNoCommandIsWaiting() {
        CompletableFuture<Void> firstGate = new CompletableFuture<>();
        CompletableFuture<Object> first = start(command("a"), "first", firstGate);
        firstGate.complete(null);
        first.join();

        CompletableFuture<Void> secondGate = new CompletableFuture<>();
        start(command("a"), "second", secondGate);

        assertEquals(List.of("first", "second"), processed);
        secondGate.complete(null);
    }

    @Test
    void aPolicyReturningNothingSkipsSequencing() {
        CommandSequencingInterceptor<CommandMessage> skipping =
                new CommandSequencingInterceptor<>((message, context) -> Optional.empty());
        CompletableFuture<Void> gate = new CompletableFuture<>();

        for (String name : List.of("first", "second")) {
            unitOfWorkFactory.create().executeWithResult(context -> {
                skipping.interceptOnHandle(command("a"), context, chain(name));
                return gate.thenApply(ignored -> (Object) name);
            });
        }

        assertEquals(List.of("first", "second"), processed);
        gate.complete(null);
    }
}
