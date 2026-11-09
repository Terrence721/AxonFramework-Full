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

package org.axonframework.messaging.tracing.attributes;

import org.axonframework.messaging.core.LegacyResources;
import org.axonframework.messaging.core.Message;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.interception.CorrelationDataInterceptor;
import org.axonframework.messaging.eventhandling.GenericEventMessage;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.axonframework.messaging.tracing.attributes.TracingTestContexts.inStartedContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link MessageIdSpanAttributesProvider}, the {@link MessageTypeSpanAttributesProvider}, the
 * {@link MetadataSpanAttributesProvider}, the {@link AggregateIdentifierSpanAttributesProvider} and the
 * {@link EntityIdSpanAttributesProvider}.
 */
class SpanAttributesProvidersTest {

    private static Message message(Map<String, String> metadata) {
        return new GenericEventMessage(new MessageType("test.event", "2.0"), "payload", metadata);
    }

    private static Message message() {
        return message(Map.of());
    }

    // --- message id ---

    @Test
    void messageIdProviderReportsTheIdentifierUnderTheDefaultKey() {
        Message message = message();

        Map<String, String> result = new MessageIdSpanAttributesProvider().provideForMessage(message, null);

        assertEquals(Map.of("axoniq.message.id", message.identifier()), result);
        assertEquals("axoniq.message.id", MessageIdSpanAttributesProvider.DEFAULT_ATTRIBUTE_KEY);
    }

    @Test
    void messageIdProviderSupportsACustomKeyAndRejectsNull() {
        Message message = message();

        assertEquals(Map.of("my.id", message.identifier()),
                     new MessageIdSpanAttributesProvider("my.id").provideForMessage(message, null));
        assertEquals("attributeKey may not be null", assertThrows(NullPointerException.class,
                () -> new MessageIdSpanAttributesProvider(null)).getMessage());
    }

    // --- message type ---

    @Test
    void messageTypeProviderReportsTheTypeUnderTheDefaultKey() {
        Message message = message();

        Map<String, String> result = new MessageTypeSpanAttributesProvider().provideForMessage(message, null);

        assertEquals(Map.of("axoniq.message.type", message.type().toString()), result);
        assertEquals("axoniq.message.type", MessageTypeSpanAttributesProvider.DEFAULT_ATTRIBUTE_KEY);
    }

    @Test
    void messageTypeProviderSupportsACustomKeyAndRejectsNull() {
        Message message = message();

        assertEquals(Map.of("my.type", message.type().toString()),
                     new MessageTypeSpanAttributesProvider("my.type").provideForMessage(message, null));
        assertEquals("attributeKey may not be null", assertThrows(NullPointerException.class,
                () -> new MessageTypeSpanAttributesProvider(null)).getMessage());
    }

    // --- metadata ---

    @Test
    void metadataProviderPrefixesAllMetadataByDefault() {
        Message message = message(Map.of("user", "alice", "tenant", "t1"));

        Map<String, String> result = new MetadataSpanAttributesProvider().provideForMessage(message, null);

        assertEquals(Map.of("axoniq.metadata.user", "alice", "axoniq.metadata.tenant", "t1"), result);
        assertEquals("axoniq.metadata.", MetadataSpanAttributesProvider.METADATA_PREFIX);
    }

    @Test
    void metadataProviderOnlyReportsTheAllowedKeysWhenGiven() {
        Message message = message(Map.of("user", "alice", "tenant", "t1"));

        Map<String, String> result =
                new MetadataSpanAttributesProvider(Set.of("user")).provideForMessage(message, null);

        assertEquals(Map.of("axoniq.metadata.user", "alice"), result);
    }

    @Test
    void metadataProviderSupportsACustomPrefix() {
        Message message = message(Map.of("user", "alice"));

        Map<String, String> result =
                new MetadataSpanAttributesProvider("custom.", Set.of()).provideForMessage(message, null);

        assertEquals(Map.of("custom.user", "alice"), result);
    }

    @Test
    void metadataProviderReportsNothingForAMessageWithoutMetadata() {
        assertTrue(new MetadataSpanAttributesProvider().provideForMessage(message(), null).isEmpty());
    }

    @Test
    void metadataProviderCopiesTheAllowedKeys() {
        Set<String> allowed = new HashSet<>(Set.of("user"));
        MetadataSpanAttributesProvider testSubject = new MetadataSpanAttributesProvider(allowed);
        allowed.add("tenant");

        Map<String, String> result =
                testSubject.provideForMessage(message(Map.of("user", "alice", "tenant", "t1")), null);

        assertEquals(Map.of("axoniq.metadata.user", "alice"), result);
    }

    @Test
    void metadataProviderRejectsNullArguments() {
        assertEquals("prefix may not be null", assertThrows(NullPointerException.class,
                () -> new MetadataSpanAttributesProvider(null, Set.of())).getMessage());
        assertEquals("allowedKeys may not be null", assertThrows(NullPointerException.class,
                () -> new MetadataSpanAttributesProvider("p.", null)).getMessage());
    }

    @Test
    void metadataProviderAlsoReportsStagedCorrelationDataFromTheContext() {
        Message message = message(Map.of("user", "alice"));

        Map<String, String> result = inStartedContext(context -> {
            context.putResource(CorrelationDataInterceptor.CORRELATION_DATA, Map.of("correlationId", "c1"));
            return new MetadataSpanAttributesProvider().provideForMessage(message, context);
        });

        assertEquals(Map.of("axoniq.metadata.user", "alice", "axoniq.metadata.correlationId", "c1"), result);
    }

    @Test
    void stagedCorrelationDataWinsOverMetadataWithTheSameKey() {
        Message message = message(Map.of("traceId", "from-metadata"));

        Map<String, String> result = inStartedContext(context -> {
            context.putResource(CorrelationDataInterceptor.CORRELATION_DATA, Map.of("traceId", "from-correlation"));
            return new MetadataSpanAttributesProvider().provideForMessage(message, context);
        });

        assertEquals(Map.of("axoniq.metadata.traceId", "from-correlation"), result);
    }

    @Test
    void allowedKeysAlsoFilterStagedCorrelationData() {
        Message message = message(Map.of("user", "alice"));

        Map<String, String> result = inStartedContext(context -> {
            context.putResource(CorrelationDataInterceptor.CORRELATION_DATA, Map.of("correlationId", "c1"));
            return new MetadataSpanAttributesProvider(Set.of("user")).provideForMessage(message, context);
        });

        assertEquals(Map.of("axoniq.metadata.user", "alice"), result);
    }

    @Test
    void aContextWithoutCorrelationDataAddsNothing() {
        Message message = message(Map.of("user", "alice"));

        Map<String, String> result = inStartedContext(
                context -> new MetadataSpanAttributesProvider().provideForMessage(message, context));

        assertEquals(Map.of("axoniq.metadata.user", "alice"), result);
    }

    // --- aggregate identifier ---

    @Test
    void aggregateIdentifierProviderReportsTheIdentifierFromTheContext() {
        Map<String, String> result = inStartedContext(context -> {
            context.putResource(LegacyResources.AGGREGATE_IDENTIFIER_KEY, "aggregate-1");
            return new AggregateIdentifierSpanAttributesProvider().provideForMessage(message(), context);
        });

        assertEquals(Map.of("axoniq.aggregate.identifier", "aggregate-1"), result);
        assertEquals("axoniq.aggregate.identifier", AggregateIdentifierSpanAttributesProvider.DEFAULT_ATTRIBUTE_KEY);
    }

    @Test
    void aggregateIdentifierProviderSupportsACustomKey() {
        Map<String, String> result = inStartedContext(context -> {
            context.putResource(LegacyResources.AGGREGATE_IDENTIFIER_KEY, "aggregate-1");
            return new AggregateIdentifierSpanAttributesProvider("my.aggregate").provideForMessage(message(), context);
        });

        assertEquals(Map.of("my.aggregate", "aggregate-1"), result);
    }

    @Test
    void aggregateIdentifierProviderReportsNothingWithoutContextOrIdentifier() {
        AggregateIdentifierSpanAttributesProvider testSubject = new AggregateIdentifierSpanAttributesProvider();

        assertTrue(testSubject.provideForMessage(message(), null).isEmpty());
        assertTrue(inStartedContext(context -> testSubject.provideForMessage(message(), context)).isEmpty());
    }

    @Test
    void aggregateIdentifierProviderRejectsANullKey() {
        assertEquals("attributeKey may not be null", assertThrows(NullPointerException.class,
                () -> new AggregateIdentifierSpanAttributesProvider(null)).getMessage());
    }

    // --- entity id ---

    @Test
    void entityIdOnlyHoldsTheSharedAttributeKey() {
        Constructor<?>[] constructors = EntityIdSpanAttributesProvider.class.getDeclaredConstructors();

        assertEquals("axoniq.entity.id", EntityIdSpanAttributesProvider.DEFAULT_ATTRIBUTE_KEY);
        assertTrue(Modifier.isFinal(EntityIdSpanAttributesProvider.class.getModifiers()));
        assertEquals(1, constructors.length);
        assertTrue(Modifier.isPrivate(constructors[0].getModifiers()));
    }
}
