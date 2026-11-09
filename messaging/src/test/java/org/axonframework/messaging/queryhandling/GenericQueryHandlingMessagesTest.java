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

import org.axonframework.common.infra.ComponentDescriptor;
import org.axonframework.conversion.Converter;
import org.axonframework.messaging.core.GenericMessage;
import org.axonframework.messaging.core.Message;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.common.TypeReference;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link GenericQueryMessage}, the {@link GenericQueryResponseMessage} and the
 * {@link GenericSubscriptionQueryUpdateMessage}.
 */
class GenericQueryHandlingMessagesTest {

    private static final MessageType TYPE = new MessageType("test.query");

    /**
     * Converts numeric strings to integers and leaves everything else as is.
     */
    private static final Converter STRING_TO_INTEGER = new Converter() {
        @SuppressWarnings("unchecked")
        @Override
        public <T> @Nullable T convert(@Nullable Object input, Type targetType) {
            if (input instanceof String text && Integer.class.equals(targetType)) {
                return (T) Integer.valueOf(text);
            }
            return (T) input;
        }

        @Override
        public void describeTo(ComponentDescriptor descriptor) {
        }
    };

    // --- GenericQueryMessage ---

    @Test
    void aQueryMessageHasTypePayloadAndNoPriorityByDefault() {
        GenericQueryMessage query = new GenericQueryMessage(TYPE, "payload");

        assertEquals(TYPE, query.type());
        assertEquals("payload", query.payload());
        assertEquals(OptionalInt.empty(), query.priority());
        assertTrue(query.metadata().isEmpty());
    }

    @Test
    void aQueryMessageCanBeCreatedFromADelegateWithAPriority() {
        Message delegate = new GenericMessage(TYPE, "payload");

        GenericQueryMessage withoutPriority = new GenericQueryMessage(delegate);
        GenericQueryMessage withPriority = new GenericQueryMessage(delegate, 5);

        assertEquals(delegate.identifier(), withoutPriority.identifier());
        assertEquals(OptionalInt.empty(), withoutPriority.priority());
        assertEquals(OptionalInt.of(5), withPriority.priority());
        assertEquals(delegate.identifier(), withPriority.identifier());
    }

    @Test
    void aQueryMessageKeepsItsIdentifierAndPriorityWhenMetadataChanges() {
        GenericQueryMessage query = new GenericQueryMessage(new GenericMessage(TYPE, "payload"), 7);

        QueryMessage replaced = query.withMetadata(Map.of("a", "1"));
        QueryMessage added = replaced.andMetadata(Map.of("b", "2"));

        assertInstanceOf(GenericQueryMessage.class, replaced);
        assertEquals(query.identifier(), replaced.identifier());
        assertEquals(OptionalInt.of(7), replaced.priority());
        assertEquals(Map.of("a", "1"), replaced.metadata());
        assertEquals(Map.of("a", "1", "b", "2"), added.metadata());
        assertEquals(OptionalInt.of(7), added.priority());
    }

    @Test
    void aQueryMessageWithAnUnchangedPayloadTypeIsReturnedAsIsWhenConverted() {
        GenericQueryMessage query = new GenericQueryMessage(TYPE, "payload");

        assertSame(query, query.withConvertedPayload(String.class, STRING_TO_INTEGER));
    }

    @Test
    void aConvertedQueryMessageKeepsIdentifierTypeMetadataAndPriority() {
        GenericQueryMessage query = new GenericQueryMessage(new GenericMessage(TYPE, "42", Map.of("k", "v")), 3);

        QueryMessage converted = query.withConvertedPayload(Integer.class, STRING_TO_INTEGER);

        assertNotSame(query, converted);
        assertEquals(42, converted.payload());
        assertEquals(query.identifier(), converted.identifier());
        assertEquals(TYPE, converted.type());
        assertEquals(Map.of("k", "v"), converted.metadata());
        assertEquals(OptionalInt.of(3), converted.priority());
    }

    @Test
    void queryConversionIsAvailableByTypeReferenceToo() {
        GenericQueryMessage query = new GenericQueryMessage(TYPE, "42");

        QueryMessage converted = query.withConvertedPayload(new TypeReference<Integer>() {
        }, STRING_TO_INTEGER);

        assertEquals(42, converted.payload());
    }

    @Test
    void aQueryMessageKeepsItsPriorityWhenGivenAConverter() {
        GenericQueryMessage query = new GenericQueryMessage(new GenericMessage(TYPE, "payload"), 4);

        GenericQueryMessage result = query.withConverter(STRING_TO_INTEGER);

        assertEquals(OptionalInt.of(4), result.priority());
        assertEquals(query.identifier(), result.identifier());
    }

    @Test
    void aQueryMessageDescribesItselfIncludingThePriority() {
        assertTrue(new GenericQueryMessage(TYPE, "payload").toString().contains("GenericQueryMessage"));
        assertTrue(new GenericQueryMessage(TYPE, "payload").toString().contains("priority='0'"));
        assertTrue(new GenericQueryMessage(new GenericMessage(TYPE, "payload"), 9).toString()
                                                                                    .contains("priority='9'"));
    }

    // --- GenericQueryResponseMessage ---

    @Test
    void aResponseHasItsResultAsPayloadAndTheTypeOfTheResultAsPayloadType() {
        GenericQueryResponseMessage response = new GenericQueryResponseMessage(TYPE, "result");

        assertEquals("result", response.payload());
        assertEquals(String.class, response.payloadType());
        assertEquals(TYPE, response.type());
        assertTrue(response.metadata().isEmpty());
    }

    @Test
    void aResponseCanDeclareItsResultType() {
        GenericQueryResponseMessage response = new GenericQueryResponseMessage(TYPE, null, String.class);

        assertNull(response.payload());
        assertEquals(String.class, response.payloadType());
    }

    @Test
    void aResponseCanBeCreatedWithMetadata() {
        GenericQueryResponseMessage inferred = new GenericQueryResponseMessage(TYPE, "result", Map.of("a", "1"));
        GenericQueryResponseMessage declared =
                new GenericQueryResponseMessage(TYPE, "result", String.class, Map.of("b", "2"));

        assertEquals(Map.of("a", "1"), inferred.metadata());
        assertEquals(Map.of("b", "2"), declared.metadata());
    }

    @Test
    void aResponseKeepsItsIdentifierWhenMetadataChanges() {
        GenericQueryResponseMessage response = new GenericQueryResponseMessage(TYPE, "result");

        QueryResponseMessage replaced = response.withMetadata(Map.of("a", "1"));
        QueryResponseMessage added = replaced.andMetadata(Map.of("b", "2"));

        assertInstanceOf(GenericQueryResponseMessage.class, replaced);
        assertEquals(response.identifier(), replaced.identifier());
        assertEquals(Map.of("a", "1", "b", "2"), added.metadata());
    }

    @Test
    void aResponseWithAnUnchangedPayloadTypeIsReturnedAsIsWhenConverted() {
        GenericQueryResponseMessage response = new GenericQueryResponseMessage(TYPE, "result");

        assertSame(response, response.withConvertedPayload(String.class, STRING_TO_INTEGER));
    }

    @Test
    void aConvertedResponseKeepsIdentifierTypeAndMetadata() {
        GenericQueryResponseMessage response = new GenericQueryResponseMessage(TYPE, "42", Map.of("k", "v"));

        QueryResponseMessage converted = response.withConvertedPayload(Integer.class, STRING_TO_INTEGER);

        assertEquals(42, converted.payload());
        assertEquals(response.identifier(), converted.identifier());
        assertEquals(TYPE, converted.type());
        assertEquals(Map.of("k", "v"), converted.metadata());
    }

    @Test
    void responseConversionIsAvailableByTypeReferenceToo() {
        QueryResponseMessage converted = new GenericQueryResponseMessage(TYPE, "42")
                .withConvertedPayload(new TypeReference<Integer>() {
                }, STRING_TO_INTEGER);

        assertEquals(42, converted.payload());
    }

    @Test
    void aResponseGivenAConverterIsStillAResponseWithTheSameIdentifier() {
        GenericQueryResponseMessage response = new GenericQueryResponseMessage(TYPE, "result");

        GenericQueryResponseMessage result = response.withConverter(STRING_TO_INTEGER);

        assertEquals(response.identifier(), result.identifier());
        assertEquals("result", result.payload());
    }

    // --- GenericSubscriptionQueryUpdateMessage ---

    @Test
    void anUpdateHasItsPayloadAndTypeAndCanDeclareItsUpdateType() {
        GenericSubscriptionQueryUpdateMessage inferred = new GenericSubscriptionQueryUpdateMessage(TYPE, "update");
        GenericSubscriptionQueryUpdateMessage declared =
                new GenericSubscriptionQueryUpdateMessage(TYPE, null, String.class);

        assertEquals("update", inferred.payload());
        assertEquals(String.class, inferred.payloadType());
        assertNull(declared.payload());
        assertEquals(String.class, declared.payloadType());
    }

    @Test
    void anUpdateCanBeCreatedWithMetadata() {
        GenericSubscriptionQueryUpdateMessage update =
                new GenericSubscriptionQueryUpdateMessage(TYPE, "update", String.class, Map.of("a", "1"));

        assertEquals(Map.of("a", "1"), update.metadata());
    }

    @Test
    void anUpdateKeepsItsIdentifierWhenMetadataChanges() {
        GenericSubscriptionQueryUpdateMessage update = new GenericSubscriptionQueryUpdateMessage(TYPE, "update");

        SubscriptionQueryUpdateMessage replaced = update.withMetadata(Map.of("a", "1"));
        SubscriptionQueryUpdateMessage added = replaced.andMetadata(Map.of("b", "2"));

        assertInstanceOf(GenericSubscriptionQueryUpdateMessage.class, replaced);
        assertEquals(update.identifier(), replaced.identifier());
        assertEquals(Map.of("a", "1", "b", "2"), added.metadata());
    }

    @Test
    void anUpdateWithAnUnchangedPayloadTypeIsReturnedAsIsWhenConverted() {
        GenericSubscriptionQueryUpdateMessage update = new GenericSubscriptionQueryUpdateMessage(TYPE, "update");

        assertSame(update, update.withConvertedPayload(String.class, STRING_TO_INTEGER));
    }

    @Test
    void aConvertedUpdateKeepsIdentifierTypeAndMetadata() {
        GenericSubscriptionQueryUpdateMessage update = new GenericSubscriptionQueryUpdateMessage(
                TYPE, "42", String.class, Map.of("k", "v"));

        SubscriptionQueryUpdateMessage converted = update.withConvertedPayload(Integer.class, STRING_TO_INTEGER);

        assertEquals(42, converted.payload());
        assertEquals(update.identifier(), converted.identifier());
        assertEquals(TYPE, converted.type());
        assertEquals(Map.of("k", "v"), converted.metadata());
    }

    @Test
    void updateConversionIsAvailableByTypeReferenceToo() {
        SubscriptionQueryUpdateMessage converted = new GenericSubscriptionQueryUpdateMessage(TYPE, "42")
                .withConvertedPayload(new TypeReference<Integer>() {
                }, STRING_TO_INTEGER);

        assertEquals(42, converted.payload());
    }

    @Test
    void anUpdateGivenAConverterIsStillAnUpdateWithTheSameIdentifier() {
        GenericSubscriptionQueryUpdateMessage update = new GenericSubscriptionQueryUpdateMessage(TYPE, "update");

        GenericSubscriptionQueryUpdateMessage result = update.withConverter(STRING_TO_INTEGER);

        assertEquals(update.identifier(), result.identifier());
    }

    @Test
    void anUpdateNamesItsTypeWhenDescribed() {
        assertTrue(new GenericSubscriptionQueryUpdateMessage(TYPE, "update").toString()
                                                                             .contains("GenericSubscriptionQueryUpdateMessage"));
    }
}
