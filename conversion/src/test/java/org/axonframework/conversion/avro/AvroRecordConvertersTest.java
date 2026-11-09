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

package org.axonframework.conversion.avro;

import org.apache.avro.generic.GenericRecord;
import org.apache.avro.message.SchemaStore;
import org.axonframework.conversion.ConversionException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link ByteArrayToGenericRecordConverter} and the {@link GenericRecordToByteArrayConverter}
 * together, as they are each other's inverse.
 */
class AvroRecordConvertersTest {

    private final GenericRecordToByteArrayConverter toBytes = new GenericRecordToByteArrayConverter();

    private static SchemaStore storeWith(org.apache.avro.Schema... schemas) {
        SchemaStore.Cache cache = new SchemaStore.Cache();
        Arrays.asList(schemas).forEach(cache::addSchema);
        return cache;
    }

    @Test
    void toBytesDeclaresItsSourceAndTargetTypes() {
        assertEquals(GenericRecord.class, toBytes.expectedSourceType());
        assertEquals(byte[].class, toBytes.targetType());
    }

    @Test
    void toRecordDeclaresItsSourceAndTargetTypes() {
        ByteArrayToGenericRecordConverter testSubject = new ByteArrayToGenericRecordConverter(storeWith());

        assertEquals(byte[].class, testSubject.expectedSourceType());
        assertEquals(GenericRecord.class, testSubject.targetType());
    }

    @Test
    void toRecordRejectsANullSchemaStore() {
        NullPointerException exception =
                assertThrows(NullPointerException.class, () -> new ByteArrayToGenericRecordConverter(null));

        assertEquals("The SchemaStore may not be null.", exception.getMessage());
    }

    @Test
    void toBytesProducesSingleObjectEncodingWithTheLibraryHeader() throws IOException {
        GenericRecord record = AvroTestFixtures.person("Alice");

        byte[] result = toBytes.convert(record);

        assertArrayEquals(AvroTestFixtures.singleObjectEncoded(record), result);
        assertEquals((byte) AvroUtil.MAGIC_BYTE, result[0]);
        assertEquals(AvroUtil.FORMAT_VERSION, result[1]);
        assertEquals(AvroUtil.fingerprint(AvroTestFixtures.PERSON_V1), AvroUtil.fingerprint(result));
    }

    @Test
    void aRecordSurvivesARoundTripThroughBytes() {
        ByteArrayToGenericRecordConverter toRecord =
                new ByteArrayToGenericRecordConverter(storeWith(AvroTestFixtures.PERSON_V3_REQUIRED_FIELD));
        GenericRecord original = AvroTestFixtures.person("Bob", 42);

        GenericRecord result = toRecord.convert(toBytes.convert(original));

        assertEquals(original, result);
        assertEquals("Bob", result.get("name").toString());
        assertEquals(42, result.get("age"));
        assertEquals(AvroTestFixtures.PERSON_V3_REQUIRED_FIELD, result.getSchema());
    }

    @Test
    void toRecordPicksTheWriterSchemaFromTheStoreByFingerprint() {
        ByteArrayToGenericRecordConverter toRecord = new ByteArrayToGenericRecordConverter(
                storeWith(AvroTestFixtures.PERSON_V1, AvroTestFixtures.PERSON_V3_REQUIRED_FIELD));

        GenericRecord v1 = toRecord.convert(toBytes.convert(AvroTestFixtures.person("Alice")));
        GenericRecord v3 = toRecord.convert(toBytes.convert(AvroTestFixtures.person("Bob", 7)));

        assertEquals(AvroTestFixtures.PERSON_V1, v1.getSchema());
        assertEquals(AvroTestFixtures.PERSON_V3_REQUIRED_FIELD, v3.getSchema());
    }

    @Test
    void nullInputConvertsToNullInBothDirections() {
        assertNull(toBytes.convert(null));
        assertNull(new ByteArrayToGenericRecordConverter(storeWith()).convert(null));
    }

    @Test
    void toRecordRejectsBytesWithoutAValidHeader() {
        ByteArrayToGenericRecordConverter toRecord =
                new ByteArrayToGenericRecordConverter(storeWith(AvroTestFixtures.PERSON_V1));

        assertThrows(RuntimeException.class, () -> toRecord.convert(new byte[]{1, 2, 3}));
    }

    @Test
    void toRecordFailsWithAConversionExceptionWhenThePayloadIsTruncated() throws IOException {
        byte[] encoded = AvroTestFixtures.singleObjectEncoded(AvroTestFixtures.person("Alice"));
        byte[] truncated = Arrays.copyOf(encoded, AvroUtil.AVRO_HEADER_LENGTH + 1);
        ByteArrayToGenericRecordConverter toRecord =
                new ByteArrayToGenericRecordConverter(storeWith(AvroTestFixtures.PERSON_V1));

        ConversionException exception = assertThrows(ConversionException.class, () -> toRecord.convert(truncated));

        assertEquals("Cannot convert bytes to GenericRecord.", exception.getMessage());
    }

    @Test
    void toRecordFailsWithAConversionExceptionWhenTheWriterSchemaIsUnknown() throws IOException {
        byte[] encoded = AvroTestFixtures.singleObjectEncoded(AvroTestFixtures.person("Alice"));
        ByteArrayToGenericRecordConverter toRecord = new ByteArrayToGenericRecordConverter(storeWith());

        ConversionException exception = assertThrows(ConversionException.class, () -> toRecord.convert(encoded));

        assertTrue(exception.getMessage().contains(
                "fingerprint:" + AvroUtil.fingerprint(AvroTestFixtures.PERSON_V1)), exception.getMessage());
    }
}
