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
import org.axonframework.common.infra.ComponentDescriptor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link AvroConverterConfiguration}.
 */
class AvroConverterConfigurationTest {

    private final SchemaStore schemaStore = new SchemaStore.Cache();
    private final AvroConverterConfiguration testSubject = new AvroConverterConfiguration(schemaStore);

    @Test
    void convenienceConstructorUsesTheDefaults() {
        assertSame(schemaStore, testSubject.schemaStore());
        assertTrue(testSubject.strategies().isEmpty());
        assertTrue(testSubject.includeDefaultAvroConverterStrategies());
        assertInstanceOf(DefaultSchemaIncompatibilityChecker.class, testSubject.schemaIncompatibilityChecker());
        assertEquals(AvroConverterStrategyConfiguration.DEFAULT, testSubject.avroConverterStrategyConfiguration());
    }

    @Test
    void convenienceConstructorRejectsANullSchemaStore() {
        NullPointerException exception =
                assertThrows(NullPointerException.class, () -> new AvroConverterConfiguration(null));

        assertEquals("Schema store cannot be null", exception.getMessage());
    }

    @Test
    void canonicalConstructorRejectsNullComponents() {
        SchemaIncompatibilityChecker checker = new DefaultSchemaIncompatibilityChecker();
        AvroConverterStrategyConfiguration strategyConfiguration = AvroConverterStrategyConfiguration.DEFAULT;

        assertEquals("Avro converter strategies cannot be null", assertThrows(NullPointerException.class,
                () -> new AvroConverterConfiguration(null, true, schemaStore, checker, strategyConfiguration))
                .getMessage());
        assertEquals("Schema incompatibility checker cannot be null", assertThrows(NullPointerException.class,
                () -> new AvroConverterConfiguration(new ArrayList<>(), true, schemaStore, null, strategyConfiguration))
                .getMessage());
        assertEquals("Avro converter strategy configuration cannot be null", assertThrows(NullPointerException.class,
                () -> new AvroConverterConfiguration(new ArrayList<>(), true, schemaStore, checker, null))
                .getMessage());
    }

    @Test
    void requiresAStrategyWhenTheDefaultStrategiesAreExcluded() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> testSubject.includeDefaultAvroConverterStrategies(false));

        assertEquals("At least one Avro converter strategy is required and no default strategies will be used",
                     exception.getMessage());
    }

    @Test
    void defaultStrategiesCanBeExcludedOnceACustomStrategyWasAdded() {
        AvroConverterConfiguration result = testSubject.addConverterStrategy(new NoOpStrategy())
                                                       .includeDefaultAvroConverterStrategies(false);

        assertFalse(result.includeDefaultAvroConverterStrategies());
        assertEquals(1, result.strategies().size());
    }

    @Test
    void addConverterStrategyReturnsANewConfigurationAndLeavesTheOriginalUntouched() {
        NoOpStrategy strategy = new NoOpStrategy();

        AvroConverterConfiguration result = testSubject.addConverterStrategy(strategy);

        assertNotSame(testSubject, result);
        assertTrue(testSubject.strategies().isEmpty());
        assertEquals(List.of(strategy), result.strategies());
    }

    @Test
    void strategiesAreKeptInTheOrderTheyWereAdded() {
        NoOpStrategy first = new NoOpStrategy();
        NoOpStrategy second = new NoOpStrategy();

        AvroConverterConfiguration result = testSubject.addConverterStrategy(first).addConverterStrategy(second);

        assertSame(first, result.strategies().get(0));
        assertSame(second, result.strategies().get(1));
    }

    @Test
    void addConverterStrategyRejectsNull() {
        NullPointerException exception =
                assertThrows(NullPointerException.class, () -> testSubject.addConverterStrategy(null));

        assertEquals("Avro converter strategy cannot be null", exception.getMessage());
    }

    @Test
    void schemaIncompatibilityCheckerCanBeReplacedButNotWithNull() {
        SchemaIncompatibilityChecker custom = new SchemaIncompatibilityChecker() {
        };

        AvroConverterConfiguration result = testSubject.schemaIncompatibilityChecker(custom);

        assertSame(custom, result.schemaIncompatibilityChecker());
        assertEquals("Schema incompatibility checker cannot be null", assertThrows(NullPointerException.class,
                () -> testSubject.schemaIncompatibilityChecker(null)).getMessage());
    }

    @Test
    void includeSchemasInStackTracesChangesOnlyThatStrategyOption() {
        AvroConverterConfiguration result = testSubject.performAvroCompatibilityCheck(false)
                                                       .includeSchemasInStackTraces(true);

        assertEquals(new AvroConverterStrategyConfiguration(false, true), result.avroConverterStrategyConfiguration());
    }

    @Test
    void performAvroCompatibilityCheckChangesOnlyThatStrategyOption() {
        AvroConverterConfiguration result = testSubject.includeSchemasInStackTraces(true)
                                                       .performAvroCompatibilityCheck(false);

        assertEquals(new AvroConverterStrategyConfiguration(false, true), result.avroConverterStrategyConfiguration());
    }

    @Test
    void modifyingOneOptionKeepsTheOthersOfTheConfiguration() {
        NoOpStrategy strategy = new NoOpStrategy();
        SchemaIncompatibilityChecker custom = new SchemaIncompatibilityChecker() {
        };
        AvroConverterConfiguration configured = testSubject.addConverterStrategy(strategy)
                                                           .schemaIncompatibilityChecker(custom);

        AvroConverterConfiguration result = configured.includeSchemasInStackTraces(true);

        assertEquals(List.of(strategy), result.strategies());
        assertSame(custom, result.schemaIncompatibilityChecker());
        assertSame(schemaStore, result.schemaStore());
        assertTrue(result.includeDefaultAvroConverterStrategies());
    }

    private static void assertInstanceOf(Class<?> expected, Object actual) {
        org.junit.jupiter.api.Assertions.assertInstanceOf(expected, actual);
    }

    private static final class NoOpStrategy implements AvroConverterStrategy {

        @Override
        public boolean test(Class<?> payloadType) {
            return false;
        }

        @Override
        public byte[] convertToSingleObjectEncoded(Object object) {
            throw new UnsupportedOperationException();
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
        public void describeTo(ComponentDescriptor descriptor) {
        }
    }
}
