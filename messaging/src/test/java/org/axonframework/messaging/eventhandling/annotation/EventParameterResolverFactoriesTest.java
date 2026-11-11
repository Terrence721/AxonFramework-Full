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

package org.axonframework.messaging.eventhandling.annotation;

import org.axonframework.common.configuration.AxonConfiguration;
import org.axonframework.messaging.core.EmptyApplicationContext;
import org.axonframework.messaging.core.LegacyResources;
import org.axonframework.messaging.core.Message;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.annotation.ParameterResolver;
import org.axonframework.messaging.core.annotation.ParameterResolverFactory;
import org.axonframework.messaging.core.configuration.MessagingConfigurer;
import org.axonframework.messaging.core.unitofwork.ProcessingContext;
import org.axonframework.messaging.core.unitofwork.SimpleUnitOfWorkFactory;
import org.axonframework.messaging.eventhandling.GenericEventMessage;
import org.axonframework.messaging.eventhandling.gateway.EventAppender;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link TimestampParameterResolverFactory}, the
 * {@link SequenceNumberParameterResolverFactory}, the {@link EventAppenderParameterResolverFactory} and the
 * {@link EventAppenderParameterResolverFactoryConfigurationEnhancer}.
 */
class EventParameterResolverFactoriesTest {

    private final SimpleUnitOfWorkFactory unitOfWorkFactory = new SimpleUnitOfWorkFactory(EmptyApplicationContext.INSTANCE);

    @SuppressWarnings("java:S119")
    void handler(@Timestamp Instant timestamp,
                 @Timestamp String wrongType,
                 Instant unannotated,
                 @SequenceNumber Long sequence,
                 @SequenceNumber long primitiveSequence,
                 @SequenceNumber String wrongSequenceType,
                 EventAppender appender,
                 String other) {
    }

    private Method handlerMethod() throws NoSuchMethodException {
        return getClass().getDeclaredMethod("handler", Instant.class, String.class, Instant.class, Long.class,
                                            long.class, String.class, EventAppender.class, String.class);
    }

    private <R> R inContext(Function<ProcessingContext, R> work) {
        return unitOfWorkFactory.create()
                                .executeWithResult(context -> CompletableFuture.completedFuture(work.apply(context)))
                                .join();
    }

    private ParameterResolver<?> resolver(ParameterResolverFactory factory, int index) throws NoSuchMethodException {
        Method method = handlerMethod();
        return factory.createInstance(method, method.getParameters(), index);
    }

    @Test
    void timestampFactoryOnlyResolvesAnnotatedInstantParameters() throws NoSuchMethodException {
        TimestampParameterResolverFactory factory = new TimestampParameterResolverFactory();

        assertNotNull(resolver(factory, 0));
        assertNull(resolver(factory, 1));
        assertNull(resolver(factory, 2));
    }

    @Test
    void timestampResolverReturnsTheEventTimestamp() throws Exception {
        ParameterResolver<?> resolver = resolver(new TimestampParameterResolverFactory(), 0);
        GenericEventMessage event = new GenericEventMessage(new MessageType("e"), "p");

        Object resolved = inContext(ctx -> {
            ProcessingContext withEvent = Message.addToContext(ctx, event);
            assertTrue(resolver.matches(withEvent));
            return resolver.resolveParameterValue(withEvent).join();
        });

        assertEquals(event.timestamp(), resolved);
    }

    @Test
    void timestampResolverDoesNotMatchWithoutAnEventInContext() throws Exception {
        ParameterResolver<?> resolver = resolver(new TimestampParameterResolverFactory(), 0);

        boolean matches = inContext(resolver::matches);

        assertFalse(matches);
        assertNull(inContext(ctx -> resolver.resolveParameterValue(ctx).join()));
    }

    @Test
    void timestampResolverDoesNotMatchForNonEventMessages() throws Exception {
        ParameterResolver<?> resolver = resolver(new TimestampParameterResolverFactory(), 0);
        org.axonframework.messaging.core.GenericMessage message =
                new org.axonframework.messaging.core.GenericMessage(new MessageType("m"), "p");

        boolean matches = inContext(ctx -> resolver.matches(Message.addToContext(ctx, message)));

        assertFalse(matches);
    }

    @Test
    void sequenceNumberFactoryResolvesLongAndPrimitiveParametersOnly() throws NoSuchMethodException {
        SequenceNumberParameterResolverFactory factory = new SequenceNumberParameterResolverFactory();

        assertNotNull(resolver(factory, 3));
        assertNotNull(resolver(factory, 4));
        assertNull(resolver(factory, 5));
        assertNull(resolver(factory, 7));
    }

    @Test
    void sequenceNumberResolverReadsTheLegacyResource() throws Exception {
        ParameterResolver<?> resolver = resolver(new SequenceNumberParameterResolverFactory(), 3);

        Object resolved = inContext(ctx -> {
            ProcessingContext withSequence = ctx.withResource(LegacyResources.AGGREGATE_SEQUENCE_NUMBER_KEY, 42L);
            assertTrue(resolver.matches(withSequence));
            return resolver.resolveParameterValue(withSequence).join();
        });

        assertEquals(42L, resolved);
    }

    @Test
    void sequenceNumberResolverDoesNotMatchWithoutTheResource() throws Exception {
        ParameterResolver<?> resolver = resolver(new SequenceNumberParameterResolverFactory(), 3);

        boolean matches = inContext(resolver::matches);

        assertFalse(matches);
    }

    @Test
    void appenderFactoryOnlyResolvesEventAppenderParameters() throws NoSuchMethodException {
        EventAppenderParameterResolverFactory factory = new EventAppenderParameterResolverFactory();

        assertNotNull(resolver(factory, 6));
        assertNull(resolver(factory, 7));
    }

    @Test
    void appenderResolverAlwaysMatches() throws Exception {
        ParameterResolver<?> resolver = resolver(new EventAppenderParameterResolverFactory(), 6);

        boolean matches = inContext(resolver::matches);

        assertTrue(matches);
    }

    @Test
    void enhancerRegistersAFactoryResolvingEventAppenderParameters() throws NoSuchMethodException {
        AxonConfiguration configuration = MessagingConfigurer.create()
                                                             .componentRegistry(
                                                                     registry -> new EventAppenderParameterResolverFactoryConfigurationEnhancer().enhance(
                                                                             registry))
                                                             .build();
        Method method = handlerMethod();

        ParameterResolverFactory factory = configuration.getComponent(ParameterResolverFactory.class);

        assertNotNull(factory.createInstance(method, method.getParameters(), 6));
        assertNull(factory.createInstance(method, method.getParameters(), 7));
    }
}
