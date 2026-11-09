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

import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Conversions;
import org.apache.avro.InvalidAvroMagicException;
import org.apache.avro.InvalidNumberEncodingException;
import org.apache.avro.Schema;
import org.apache.avro.SchemaCompatibility;
import org.apache.avro.generic.GenericData;
import org.apache.avro.message.BadHeaderException;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificRecordBase;
import org.axonframework.conversion.ConversionException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link AvroUtil}.
 */
class AvroUtilTest {

    // --- constants ---

    @Test
    void definesTheSingleObjectEncodingHeader() {
        assertEquals(0xC3, AvroUtil.MAGIC_BYTE);
        assertEquals(0x01, AvroUtil.FORMAT_VERSION);
        assertEquals(8, AvroUtil.AVRO_FINGERPRINT_LENGTH);
        assertEquals(10, AvroUtil.AVRO_HEADER_LENGTH);
    }

    // --- fingerprint ---

    @Test
    void fingerprintOfEncodedBytesEqualsTheFingerprintOfTheWriterSchema() throws IOException {
        byte[] encoded = AvroTestFixtures.singleObjectEncoded(AvroTestFixtures.person("Alice"));

        assertEquals(AvroUtil.fingerprint(AvroTestFixtures.PERSON_V1), AvroUtil.fingerprint(encoded));
    }

    @Test
    void differentSchemasHaveDifferentFingerprints() {
        assertNotEquals(AvroUtil.fingerprint(AvroTestFixtures.PERSON_V1),
                        AvroUtil.fingerprint(AvroTestFixtures.PERSON_V2_WITH_DEFAULT));
    }

    @Test
    void fingerprintRejectsBytesWithoutTheMagicByte() {
        byte[] bytes = new byte[AvroUtil.AVRO_HEADER_LENGTH];
        bytes[0] = 0x00;
        bytes[1] = AvroUtil.FORMAT_VERSION;

        AvroRuntimeException exception =
                assertThrows(AvroRuntimeException.class, () -> AvroUtil.fingerprint(bytes));

        assertEquals("Could not read fingerprint from byte array", exception.getMessage());
        assertInstanceOf(InvalidAvroMagicException.class, exception.getCause());
        assertTrue(exception.getCause().getMessage().contains("0x00"), exception.getCause().getMessage());
    }

    @Test
    void fingerprintRejectsEmptyBytes() {
        AvroRuntimeException exception =
                assertThrows(AvroRuntimeException.class, () -> AvroUtil.fingerprint(new byte[0]));

        assertInstanceOf(InvalidAvroMagicException.class, exception.getCause());
    }

    @Test
    void fingerprintRejectsAnUnknownFormatVersion() {
        byte[] bytes = new byte[AvroUtil.AVRO_HEADER_LENGTH];
        bytes[0] = (byte) AvroUtil.MAGIC_BYTE;
        bytes[1] = 0x02;

        AvroRuntimeException exception =
                assertThrows(AvroRuntimeException.class, () -> AvroUtil.fingerprint(bytes));

        assertInstanceOf(InvalidNumberEncodingException.class, exception.getCause());
        assertTrue(exception.getCause().getMessage().contains("0x02"), exception.getCause().getMessage());
    }

    @Test
    void fingerprintRejectsAHeaderThatIsTooShort() {
        byte[] bytes = {(byte) AvroUtil.MAGIC_BYTE, AvroUtil.FORMAT_VERSION, 1, 2, 3};

        assertThrows(BadHeaderException.class, () -> AvroUtil.fingerprint(bytes));
    }

    // --- payload ---

    @Test
    void payloadIsEverythingBehindTheHeader() throws IOException {
        byte[] encoded = AvroTestFixtures.singleObjectEncoded(AvroTestFixtures.person("Alice"));

        byte[] payload = AvroUtil.payload(encoded);

        assertArrayEquals(Arrays.copyOfRange(encoded, AvroUtil.AVRO_HEADER_LENGTH, encoded.length), payload);
        assertEquals(encoded.length - AvroUtil.AVRO_HEADER_LENGTH, payload.length);
    }

    @Test
    void payloadOfAHeaderOnlyMessageIsEmpty() {
        assertEquals(0, AvroUtil.payload(new byte[AvroUtil.AVRO_HEADER_LENGTH]).length);
    }

    @Test
    void payloadRejectsBytesShorterThanTheHeader() {
        assertThrows(BadHeaderException.class, () -> AvroUtil.payload(new byte[AvroUtil.AVRO_HEADER_LENGTH - 1]));
    }

    // --- specific data ---

    @Test
    void fromSpecificDataCopiesTheLogicalTypeConversions() {
        SpecificData specificData = new SpecificData();
        Conversions.DecimalConversion decimalConversion = new Conversions.DecimalConversion();
        specificData.addLogicalTypeConversion(decimalConversion);

        GenericData result = AvroUtil.fromSpecificData(specificData);

        assertNotNull(result.getConversionByClass(BigDecimal.class));
        assertSame(decimalConversion, result.getConversionByClass(BigDecimal.class));
    }

    @Test
    void fromSpecificDataWithoutConversionsHasNone() {
        GenericData result = AvroUtil.fromSpecificData(new SpecificData());

        assertNull(result.getConversionByClass(BigDecimal.class));
    }

    @Test
    void classSchemaIsReadFromTheStaticMethodOfASpecificRecord() {
        @SuppressWarnings("unchecked")
        Class<SpecificRecordBase> type = (Class<SpecificRecordBase>) (Class<?>) TestSpecificRecord.class;

        assertSame(TestSpecificRecord.SCHEMA, AvroUtil.getClassSchemaChecked(type));
    }

    @Test
    void classSchemaLookupFailureNamesTheOffendingClass() {
        @SuppressWarnings("unchecked")
        Class<SpecificRecordBase> type = (Class<SpecificRecordBase>) (Class<?>) RecordWithoutClassSchema.class;

        AvroRuntimeException exception = assertThrows(AvroRuntimeException.class,
                                                      () -> AvroUtil.getClassSchemaChecked(type));

        assertTrue(exception.getMessage().contains(RecordWithoutClassSchema.class.getCanonicalName()),
                   exception.getMessage());
        assertNotNull(exception.getCause());
    }

    // --- compatibility ---

    @Test
    void identicalSchemasHaveNoIncompatibilities() {
        assertTrue(AvroUtil.checkCompatibility(AvroTestFixtures.PERSON_V1, AvroTestFixtures.PERSON_V1).isEmpty());
    }

    @Test
    void aReaderWithAnExtraFieldThatHasADefaultIsCompatible() {
        assertTrue(AvroUtil.checkCompatibility(AvroTestFixtures.PERSON_V2_WITH_DEFAULT,
                                               AvroTestFixtures.PERSON_V1).isEmpty());
    }

    @Test
    void aReaderWithARequiredFieldTheWriterLacksIsIncompatible() {
        List<SchemaCompatibility.Incompatibility> result =
                AvroUtil.checkCompatibility(AvroTestFixtures.PERSON_V3_REQUIRED_FIELD, AvroTestFixtures.PERSON_V1);

        assertFalse(result.isEmpty());
        assertEquals(SchemaCompatibility.SchemaIncompatibilityType.READER_FIELD_MISSING_DEFAULT_VALUE,
                     result.get(0).getType());
    }

    @Test
    void incompatibilityPrinterShowsTypeLocationAndMessage() {
        SchemaCompatibility.Incompatibility incompatibility =
                AvroUtil.checkCompatibility(AvroTestFixtures.PERSON_V3_REQUIRED_FIELD, AvroTestFixtures.PERSON_V1)
                        .get(0);

        String printed = AvroUtil.incompatibilityPrinter(incompatibility);

        assertEquals("READER_FIELD_MISSING_DEFAULT_VALUE located at \"" + incompatibility.getLocation()
                             + "\" with value \"" + incompatibility.getMessage() + "\"", printed);
    }

    // --- exceptions ---

    @Test
    void failedToDeserializeMessageDescribesReaderWriterAndIncompatibilities() {
        ConversionException exception = AvroUtil.createExceptionFailedToDeserialize(
                String.class, AvroTestFixtures.PERSON_V3_REQUIRED_FIELD, AvroTestFixtures.PERSON_V1,
                "[some problem]", false);

        String message = exception.getMessage();
        assertTrue(message.startsWith("Failed to deserialize single-object-encoded bytes to instance of java.lang.String"));
        assertTrue(message.contains("writer schema fingerprint is "
                                            + AvroUtil.fingerprint(AvroTestFixtures.PERSON_V1)));
        assertTrue(message.contains("reader schema fingerprint is "
                                            + AvroUtil.fingerprint(AvroTestFixtures.PERSON_V3_REQUIRED_FIELD)));
        assertTrue(message.contains("detected incompatibilities are: [some problem]"));
        assertTrue(message.endsWith("Consider to define an upcaster to fix this problem."));
        assertFalse(message.contains("writer schema is "));
        assertFalse(message.contains("reader schema is "));
    }

    @Test
    void failedToDeserializeMessageIncludesTheSchemasOnlyWhenAsked() {
        ConversionException exception = AvroUtil.createExceptionFailedToDeserialize(
                String.class, AvroTestFixtures.PERSON_V3_REQUIRED_FIELD, AvroTestFixtures.PERSON_V1,
                "[some problem]", true);

        assertTrue(exception.getMessage().contains("writer schema is " + AvroTestFixtures.PERSON_V1));
        assertTrue(exception.getMessage().contains("reader schema is " + AvroTestFixtures.PERSON_V3_REQUIRED_FIELD));
    }

    @Test
    void failedToDeserializeWithACauseKeepsTheCause() {
        IOException cause = new IOException("broken");

        ConversionException exception = AvroUtil.createExceptionFailedToDeserialize(
                String.class, AvroTestFixtures.PERSON_V1, AvroTestFixtures.PERSON_V1, cause, false);

        assertSame(cause, exception.getCause());
        assertTrue(exception.getMessage().contains("java.lang.String"));
    }

    @Test
    void noSchemaFoundMessageNamesTheTypeAndFingerprint() {
        ConversionException exception = AvroUtil.createExceptionNoSchemaFound(String.class, 12345L);

        assertInstanceOf(ConversionException.class, exception);
        assertTrue(exception.getMessage().contains("java.lang.String"));
        assertTrue(exception.getMessage().contains("fingerprint:12345"));
    }

    private static void assertNotEquals(long unexpected, long actual) {
        org.junit.jupiter.api.Assertions.assertNotEquals(unexpected, actual);
    }

    /**
     * A minimal hand-written specific record, so no Avro code generation is needed.
     */
    public static class TestSpecificRecord extends SpecificRecordBase {

        static final Schema SCHEMA = AvroTestFixtures.PERSON_V1;

        public static Schema getClassSchema() {
            return SCHEMA;
        }

        @Override
        public Schema getSchema() {
            return SCHEMA;
        }

        @Override
        public Object get(int field) {
            return null;
        }

        @Override
        public void put(int field, Object value) {
        }
    }

    /**
     * A specific record that does not provide the static {@code getClassSchema} method.
     */
    public static class RecordWithoutClassSchema extends SpecificRecordBase {

        @Override
        public Schema getSchema() {
            return AvroTestFixtures.PERSON_V1;
        }

        @Override
        public Object get(int field) {
            return null;
        }

        @Override
        public void put(int field, Object value) {
        }
    }
}
