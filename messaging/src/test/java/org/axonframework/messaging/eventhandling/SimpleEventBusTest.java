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
package org.axonframework.messaging.eventhandling;

import org.axonframework.common.FutureUtils;
import org.axonframework.common.Registration;
import org.axonframework.messaging.core.EmptyApplicationContext;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.unitofwork.ProcessingContext;
import org.axonframework.messaging.core.unitofwork.SimpleUnitOfWorkFactory;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link SimpleEventBus}, including its use of {@link EventSubscribers}.
 */
class SimpleEventBusTest {

    private static final MessageType TYPE = new MessageType("test.event");

    private final SimpleUnitOfWorkFactory unitOfWorkFactory =
            new SimpleUnitOfWorkFactory(EmptyApplicationContext.INSTANCE);
    private final SimpleEventBus bus = new SimpleEventBus();
    private final List<List<? extends EventMessage>> batches = new ArrayList<>();

    private static EventMessage event(String payload) {
        return new GenericEventMessage(TYPE, payload);
    }

    private Registration subscribeRecording() {
        return bus.subscribe((events, context) -> {
            batches.add(events);
            return FutureUtils.emptyCompletedFuture();
        });
    }

    @Test
    void publishWithoutContextNotifiesSubscribersImmediately() {
        subscribeRecording();
        EventMessage first = event("one");
        EventMessage second = event("two");

        bus.publish(null, List.of(first, second)).join();

        assertEquals(1, batches.size());
        assertEquals(List.of(first, second), batches.get(0));
    }

    @Test
    void publishWithoutContextPropagatesSubscriberFailure() {
        IllegalStateException error = new IllegalStateException("boom");
        bus.subscribe((events, context) -> CompletableFuture.failedFuture(error));

        CompletionException failure =
                assertThrows(CompletionException.class, () -> bus.publish(null, List.of(event("x"))).join());

        assertSame(error, failure.getCause());
    }

    @Test
    void publishWithoutContextReportsThrowingSubscriberAndStillNotifiesTheOthers() {
        IllegalStateException error = new IllegalStateException("thrown");
        bus.subscribe((events, context) -> {
            throw error;
        });
        subscribeRecording();

        CompletionException failure =
                assertThrows(CompletionException.class, () -> bus.publish(null, List.of(event("x"))).join());

        assertSame(error, failure.getCause());
        assertEquals(1, batches.size());
    }

    @Test
    void publishWithoutSubscribersCompletes() {
        bus.publish(null, List.of(event("x"))).join();
    }

    @Test
    void publishWithVarargsDelegatesToTheListVariant() {
        subscribeRecording();
        EventMessage only = event("only");

        bus.publish(null, only).join();

        assertEquals(List.of(only), batches.get(0));
    }

    @Test
    void publishWithContextIsDeferredUntilPrepareCommit() {
        subscribeRecording();
        List<Integer> batchCountDuringInvocation = new ArrayList<>();

        unitOfWorkFactory.create().executeWithResult(ctx -> {
            bus.publish(ctx, List.of(event("queued"))).join();
            batchCountDuringInvocation.add(batches.size());
            return FutureUtils.emptyCompletedFuture();
        }).join();

        assertEquals(List.of(0), batchCountDuringInvocation);
        assertEquals(1, batches.size());
    }

    @Test
    void eventsPublishedInTheSameContextAreDeliveredAsASingleBatch() {
        subscribeRecording();
        EventMessage first = event("one");
        EventMessage second = event("two");

        unitOfWorkFactory.create().executeWithResult(ctx -> {
            bus.publish(ctx, List.of(first));
            bus.publish(ctx, List.of(second));
            return FutureUtils.emptyCompletedFuture();
        }).join();

        assertEquals(1, batches.size());
        assertEquals(List.of(first, second), batches.get(0));
    }

    @Test
    void eventsPublishedBySubscribersDuringPrepareCommitAreAlsoDelivered() {
        EventMessage follow = event("follow-up");
        List<EventMessage> seen = new ArrayList<>();
        bus.subscribe((events, context) -> {
            seen.addAll(events);
            if (events.stream().anyMatch(e -> "initial".equals(e.payload()))) {
                bus.publish(context, List.of(follow));
            }
            return FutureUtils.emptyCompletedFuture();
        });

        unitOfWorkFactory.create().executeWithResult(ctx -> {
            bus.publish(ctx, List.of(event("initial")));
            return FutureUtils.emptyCompletedFuture();
        }).join();

        assertEquals(2, seen.size());
        assertSame(follow, seen.get(1));
    }

    @Test
    void eventsAreNotDeliveredWhenTheContextFails() {
        subscribeRecording();

        assertThrows(CompletionException.class, () -> unitOfWorkFactory.create().executeWithResult(ctx -> {
            bus.publish(ctx, List.of(event("discarded")));
            return CompletableFuture.failedFuture(new IllegalStateException("rollback"));
        }).join());

        assertTrue(batches.isEmpty());
    }

    @Test
    void publishingAfterCommitIsRejected() {
        List<Throwable> rejected = new ArrayList<>();

        unitOfWorkFactory.create().executeWithResult(ctx -> {
            ctx.onAfterCommit(committed -> {
                try {
                    bus.publish(committed, List.of(event("late")));
                } catch (IllegalStateException e) {
                    rejected.add(e);
                }
                return FutureUtils.emptyCompletedFuture();
            });
            return FutureUtils.emptyCompletedFuture();
        }).join();

        assertEquals(1, rejected.size());
    }

    @Test
    void unsubscribingStopsNotifications() {
        Registration registration = subscribeRecording();

        assertTrue(registration.cancel());
        bus.publish(null, List.of(event("x"))).join();

        assertTrue(batches.isEmpty());
        assertFalse(registration.cancel());
    }

    @Test
    void subscribingTheSameConsumerTwiceNotifiesItOnce() {
        BiFunction<List<? extends EventMessage>, ProcessingContext, CompletableFuture<?>> consumer =
                (events, context) -> {
                    batches.add(events);
                    return FutureUtils.emptyCompletedFuture();
                };
        bus.subscribe(consumer);
        bus.subscribe(consumer);

        bus.publish(null, List.of(event("x"))).join();

        assertEquals(1, batches.size());
    }
}
