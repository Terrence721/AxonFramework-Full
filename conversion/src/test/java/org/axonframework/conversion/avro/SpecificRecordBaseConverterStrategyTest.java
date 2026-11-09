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

import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.message.SchemaStore;
import org.axonframework.common.AxonConfigurationException;
import org.axonframework.common.infra.FilesystemStyleComponentDescriptor;
import org.axonframework.conversion.ConversionException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link SpecificRecordBaseConverterStrategy}, using hand-written specific records.
 */
class SpecificRecordBaseConverterStrategyTest {

    private final SchemaStore.Cache schemaStore = new SchemaStore.Cache();
    private final SchemaIncompatibilityChecker checker = new DefaultSchemaIncompatibilityChecker();
    private final SpecificRecordBaseConverterStrategy testSubject =
            new SpecificRecordBaseConverterStrategy(schemaStore, checker);

    private byte[] encodedPerson(String name) {
        schemaStore.addSchema(TestPersonRecord.SCHEMA$);
        return testSubject.convertToSingleObjectEncoded(new TestPersonRecord(name));
    }

    @Test
    void constructorRejectsNullArguments() {
        assertEquals("Schema store must not be null.", assertThrows(NullPointerException.class,
                () -> new SpecificRecordBaseConverterStrategy(null, checker)).getMessage());
        assertEquals("SchemaIncompatibilityChecker must not be null.", assertThrows(NullPointerException.class,
                () -> new SpecificRecordBaseConverterStrategy(schemaStore, null)).getMessage());
    }

    @Test
    void supportsOnlySpecificRecordTypes() {
        assertTrue(testSubject.test(TestPersonRecord.class));
        assertTrue(testSubject.test(org.apache.avro.specific.SpecificRecordBase.class));
        assertFalse(testSubject.test(String.class));
        assertFalse(testSubject.test(GenericData.Record.class));
        assertFalse(testSubject.test(byte[].class));
    }

    @Test
    void convertsASpecificRecordToSingleObjectEncodedBytes() throws IOException {
        byte[] result = testSubject.convertToSingleObjectEncoded(new TestPersonRecord("Alice"));

        assertEquals((byte) AvroUtil.MAGIC_BYTE, result[0]);
        assertEquals(AvroUtil.FORMAT_VERSION, result[1]);
        assertEquals(AvroUtil.fingerprint(TestPersonRecord.SCHEMA$), AvroUtil.fingerprint(result));
    }

    @Test
    void refusesToEncodeObjectsThatAreNotSpecificRecords() {
        ConversionException exception =
                assertThrows(ConversionException.class, () -> testSubject.convertToSingleObjectEncoded("text"));

        assertEquals("Expected object to be instance of SpecificRecordBase but it was java.lang.String",
                     exception.getMessage());
    }

    @Test
    void aSpecificRecordSurvivesARoundTripThroughSingleObjectEncoding() {
        byte[] bytes = encodedPerson("Alice");

        TestPersonRecord result = testSubject.convertFromSingleObjectEncoded(bytes, TestPersonRecord.class);

        assertEquals("Alice", result.getName().toString());
    }

    @Test
    void decodingFailsForReaderTypesThatAreNotSpecificRecords() {
        byte[] bytes = encodedPerson("Alice");

        ConversionException exception = assertThrows(ConversionException.class,
                () -> testSubject.convertFromSingleObjectEncoded(bytes, String.class));

        assertEquals("Expected reader type to be assignable from SpecificRecordBase but it was java.lang.String",
                     exception.getMessage());
    }

    @Test
    void decodingFailsWhenTheWriterSchemaIsNotInTheStore() {
        byte[] bytes = testSubject.convertToSingleObjectEncoded(new TestPersonRecord("Alice"));

        ConversionException exception = assertThrows(ConversionException.class,
                () -> testSubject.convertFromSingleObjectEncoded(bytes, TestPersonRecord.class));

        assertTrue(exception.getMessage().contains("fingerprint:" + AvroUtil.fingerprint(TestPersonRecord.SCHEMA$)),
                   exception.getMessage());
    }

    @Test
    void decodingFailsForAnIncompatibleReaderAndWritersSchema() {
        schemaStore.addSchema(TestOtherRecord.SCHEMA$);
        byte[] bytes = new SpecificRecordBaseConverterStrategy(schemaStore, checker)
                .convertToSingleObjectEncoded(new TestOtherRecord() {{
                    put(0, "Alice");
                }});

        ConversionException exception = assertThrows(ConversionException.class,
                () -> testSubject.convertFromSingleObjectEncoded(bytes, TestPersonRecord.class));

        assertTrue(exception.getMessage().contains("detected incompatibilities are"), exception.getMessage());
        assertTrue(exception.getMessage().contains("Consider to define an upcaster"), exception.getMessage());
        assertFalse(exception.getMessage().contains("writer schema is "), exception.getMessage());
    }

    @Test
    void incompatibilityMessagesIncludeTheSchemasWhenConfigured() {
        schemaStore.addSchema(TestOtherRecord.SCHEMA$);
        byte[] bytes = testSubject.convertToSingleObjectEncoded(new TestOtherRecord() {{
            put(0, "Alice");
        }});
        testSubject.applyStrategyConfiguration(new AvroConverterStrategyConfiguration(true, true));

        ConversionException exception = assertThrows(ConversionException.class,
                () -> testSubject.convertFromSingleObjectEncoded(bytes, TestPersonRecord.class));

        assertTrue(exception.getMessage().contains("writer schema is "), exception.getMessage());
        assertTrue(exception.getMessage().contains("reader schema is "), exception.getMessage());
    }

    @Test
    void withTheCompatibilityCheckDisabledAnIncompatibleSchemaIsNoLongerReported() {
        schemaStore.addSchema(TestOtherRecord.SCHEMA$);
        byte[] bytes = testSubject.convertToSingleObjectEncoded(new TestOtherRecord() {{
            put(0, "Alice");
        }});
        testSubject.applyStrategyConfiguration(new AvroConverterStrategyConfiguration(false, false));

        // Avro itself does not reject the differing record names while reading, so only the check reported it
        TestPersonRecord result = testSubject.convertFromSingleObjectEncoded(bytes, TestPersonRecord.class);

        assertEquals("Alice", result.getName().toString());
    }

    @Test
    void convertsAGenericRecordToASpecificRecord() {
        GenericRecord generic = new GenericData.Record(TestPersonRecord.SCHEMA$);
        generic.put("name", "Alice");

        TestPersonRecord result = testSubject.convertFromGenericRecord(generic, TestPersonRecord.class);

        assertEquals("Alice", result.getName().toString());
    }

    @Test
    void genericRecordConversionFailsForReaderTypesThatAreNotSpecificRecords() {
        GenericRecord generic = new GenericData.Record(TestPersonRecord.SCHEMA$);

        ConversionException exception = assertThrows(ConversionException.class,
                () -> testSubject.convertFromGenericRecord(generic, String.class));

        assertEquals("Expected reader type to be assignable from SpecificRecordBase but it was java.lang.String",
                     exception.getMessage());
    }

    @Test
    void genericRecordConversionFailsForAnIncompatibleSchema() {
        GenericRecord generic = new GenericData.Record(TestOtherRecord.SCHEMA$);
        generic.put("name", "Alice");

        ConversionException exception = assertThrows(ConversionException.class,
                () -> testSubject.convertFromGenericRecord(generic, TestPersonRecord.class));

        assertTrue(exception.getMessage().contains("detected incompatibilities are"), exception.getMessage());
    }

    @Test
    void rejectsANullStrategyConfiguration() {
        AxonConfigurationException exception =
                assertThrows(AxonConfigurationException.class, () -> testSubject.applyStrategyConfiguration(null));

        assertEquals("AvroConverterConfiguration must not be null", exception.getMessage());
    }

    @Test
    void producesTheSameBytesAsTheStrategyForRepeatedConversions() {
        assertArrayEquals(testSubject.convertToSingleObjectEncoded(new TestPersonRecord("Alice")),
                          testSubject.convertToSingleObjectEncoded(new TestPersonRecord("Alice")));
    }

    @Test
    void describesItsConfigurationCheckerAndSchemaStore() {
        FilesystemStyleComponentDescriptor descriptor = new FilesystemStyleComponentDescriptor();

        testSubject.describeTo(descriptor);

        String description = descriptor.describe();
        assertTrue(description.contains("avroConverterStrategyConfiguration"), description);
        assertTrue(description.contains("schemaIncompatibilityChecker"), description);
        assertTrue(description.contains("schemaStore"), description);
    }
}
