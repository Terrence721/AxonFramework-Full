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
import org.axonframework.common.infra.ComponentDescriptor;
import org.axonframework.common.infra.FilesystemStyleComponentDescriptor;
import org.axonframework.conversion.ConversionException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link AvroConverter}, using hand-written specific records.
 */
class AvroConverterTest {

    private final SchemaStore.Cache schemaStore = new SchemaStore.Cache();
    private final AvroConverter testSubject;

    AvroConverterTest() {
        schemaStore.addSchema(TestPersonRecord.SCHEMA$);
        testSubject = new AvroConverter(schemaStore, configuration -> configuration);
    }

    private byte[] personBytes(String name) {
        return testSubject.convert(new TestPersonRecord(name), byte[].class);
    }

    // --- construction ---

    @Test
    void constructorRejectsANullConfigurationOverride() {
        NullPointerException exception =
                assertThrows(NullPointerException.class, () -> new AvroConverter(schemaStore, null));

        assertEquals("the configurationOverride may not be null.", exception.getMessage());
    }

    @Test
    void constructorRejectsANullSchemaStore() {
        assertThrows(NullPointerException.class, () -> new AvroConverter(null, configuration -> configuration));
    }

    @Test
    void constructorRejectsAConfigurationWithoutAnyStrategy() {
        assertThrows(IllegalArgumentException.class,
                     () -> new AvroConverter(schemaStore, c -> c.includeDefaultAvroConverterStrategies(false)));
    }

    @Test
    void configurationOverrideCanAddAStrategyAndTheStrategyIsConfigured() {
        AtomicReference<AvroConverterStrategyConfiguration> applied = new AtomicReference<>();
        AvroConverterStrategy custom = new FixedStrategy(StringBuilder.class, applied);

        new AvroConverter(schemaStore, c -> c.addConverterStrategy(custom)
                                             .includeSchemasInStackTraces(true)
                                             .performAvroCompatibilityCheck(false));

        assertEquals(new AvroConverterStrategyConfiguration(false, true), applied.get());
    }

    // --- trivial cases ---

    @Test
    void nullInputConvertsToNull() {
        assertNull(testSubject.convert(null, TestPersonRecord.class));
        assertNull(testSubject.convert(null, byte[].class));
    }

    @Test
    void inputThatAlreadyHasTheTargetTypeIsReturnedAsIs() {
        TestPersonRecord record = new TestPersonRecord("Alice");

        assertSame(record, testSubject.convert(record, TestPersonRecord.class));
        assertSame(record, testSubject.convert(record, org.apache.avro.specific.SpecificRecordBase.class));
        assertSame(record, testSubject.convert(record, Object.class));
    }

    @Test
    void targetTypesThatAreNotClassesAreRejected() throws NoSuchFieldException {
        Type listOfStrings = AvroConverterTest.class.getDeclaredField("typed").getGenericType();

        ConversionException exception = assertThrows(ConversionException.class,
                () -> testSubject.convert("input", listOfStrings));

        assertTrue(exception.getMessage().contains("target type must be a class"), exception.getMessage());
    }

    @SuppressWarnings({"unused", "java:S119"})
    private List<String> typed;

    // --- serialization ---

    @Test
    void convertsASpecificRecordToSingleObjectEncodedBytes() {
        byte[] bytes = personBytes("Alice");

        assertEquals((byte) AvroUtil.MAGIC_BYTE, bytes[0]);
        assertEquals(AvroUtil.fingerprint(TestPersonRecord.SCHEMA$), AvroUtil.fingerprint(bytes));
    }

    @Test
    void convertsASpecificRecordToOtherTypesThroughTheContentTypeChain() {
        InputStream stream = testSubject.convert(new TestPersonRecord("Alice"), InputStream.class);

        assertNotNull(stream);
    }

    @Test
    void serializationToAnUnreachableTargetTypeFails() {
        ConversionException exception = assertThrows(ConversionException.class,
                () -> testSubject.convert(new TestPersonRecord("Alice"), Integer.class));

        assertEquals("Cannot convert given input to target type [class java.lang.Integer]", exception.getMessage());
    }

    // --- deserialization ---

    @Test
    void convertsBytesBackToASpecificRecord() {
        TestPersonRecord result = testSubject.convert(personBytes("Alice"), TestPersonRecord.class);

        assertEquals("Alice", result.getName().toString());
    }

    @Test
    void convertsAnInputStreamBackToASpecificRecordThroughTheContentTypeChain() {
        byte[] bytes = personBytes("Alice");

        TestPersonRecord result = testSubject.convert(new ByteArrayInputStream(bytes), TestPersonRecord.class);

        assertEquals("Alice", result.getName().toString());
    }

    @Test
    void convertsAGenericRecordToASpecificRecord() {
        GenericRecord generic = new GenericData.Record(TestPersonRecord.SCHEMA$);
        generic.put("name", "Alice");

        TestPersonRecord result = testSubject.convert(generic, TestPersonRecord.class);

        assertEquals("Alice", result.getName().toString());
    }

    @Test
    void deserializationFromAnUnsupportedSourceTypeFails() {
        ConversionException exception = assertThrows(ConversionException.class,
                () -> testSubject.convert(Integer.valueOf(1), TestPersonRecord.class));

        assertEquals("Cannot convert given input to target type [class "
                             + TestPersonRecord.class.getName() + "]", exception.getMessage());
    }

    // --- generic record as intermediate representation ---

    @Test
    void convertsBytesToAGenericRecordForUpcasting() {
        GenericRecord result = testSubject.convert(personBytes("Alice"), GenericRecord.class);

        assertEquals("Alice", result.get("name").toString());
        assertEquals(TestPersonRecord.SCHEMA$, result.getSchema());
    }

    @Test
    void convertsAnInputStreamToAGenericRecordThroughTheContentTypeChain() {
        GenericRecord result = testSubject.convert(new ByteArrayInputStream(personBytes("Alice")),
                                                   GenericRecord.class);

        assertEquals("Alice", result.get("name").toString());
    }

    @Test
    void conversionToAGenericRecordFromAnUnsupportedSourceFails() {
        assertThrows(ConversionException.class, () -> testSubject.convert(Integer.valueOf(1), GenericRecord.class));
    }

    // --- everything else ---

    @Test
    void unsupportedConversionsFail() {
        ConversionException exception = assertThrows(ConversionException.class,
                () -> testSubject.convert("text", Integer.class));

        assertEquals("Cannot convert given input to target type [class java.lang.Integer]", exception.getMessage());
    }

    // --- custom strategies ---

    @Test
    void aCustomStrategyHandlesItsOwnTypesWhileTheDefaultOnesKeepWorking() {
        FixedStrategy custom = new FixedStrategy(StringBuilder.class, new AtomicReference<>());
        AvroConverter converter = new AvroConverter(schemaStore, c -> c.addConverterStrategy(custom));

        assertArrayEquals(new byte[]{9, 9}, converter.convert(new StringBuilder("x"), byte[].class));
        assertEquals("Alice", ((TestPersonRecord) converter.convert(
                converter.convert(new TestPersonRecord("Alice"), byte[].class), TestPersonRecord.class))
                .getName().toString());
    }

    @Test
    void withoutTheDefaultStrategiesSpecificRecordsAreNoLongerSupported() {
        FixedStrategy custom = new FixedStrategy(StringBuilder.class, new AtomicReference<>());
        AvroConverter converter = new AvroConverter(schemaStore, c -> c.addConverterStrategy(custom)
                                                                       .includeDefaultAvroConverterStrategies(false));

        assertThrows(ConversionException.class,
                     () -> converter.convert(new TestPersonRecord("Alice"), byte[].class));
    }

    @Test
    void describesItselfThroughItsStrategies() {
        FilesystemStyleComponentDescriptor descriptor = new FilesystemStyleComponentDescriptor();

        testSubject.describeTo(descriptor);

        assertTrue(descriptor.describe().contains("schemaStore"), descriptor.describe());
    }

    @Test
    void convertReturnsAnInstanceOfTheRequestedType() {
        assertInstanceOf(TestPersonRecord.class, testSubject.convert(personBytes("Alice"), TestPersonRecord.class));
    }

    /**
     * A strategy that handles a single type and always encodes to two fixed bytes.
     */
    private static final class FixedStrategy implements AvroConverterStrategy {

        private final Class<?> handledType;
        private final AtomicReference<AvroConverterStrategyConfiguration> appliedConfiguration;

        private FixedStrategy(Class<?> handledType,
                              AtomicReference<AvroConverterStrategyConfiguration> appliedConfiguration) {
            this.handledType = handledType;
            this.appliedConfiguration = appliedConfiguration;
        }

        @Override
        public boolean test(Class<?> payloadType) {
            return handledType.equals(payloadType);
        }

        @Override
        public byte[] convertToSingleObjectEncoded(Object object) {
            return new byte[]{9, 9};
        }

        @Override
        public <T> T convertFromSingleObjectEncoded(byte[] bytes, Class<T> type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T convertFromGenericRecord(GenericRecord genericRecord, Class<T> type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void applyStrategyConfiguration(AvroConverterStrategyConfiguration configuration) {
            appliedConfiguration.set(configuration);
        }

        @Override
        public void describeTo(ComponentDescriptor descriptor) {
            descriptor.describeProperty("custom", "fixed");
        }
    }
}
