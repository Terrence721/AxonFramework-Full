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

import org.apache.avro.Schema;
import org.apache.avro.SchemaCompatibility;
import org.axonframework.conversion.ConversionException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the default methods of the {@link SchemaIncompatibilityChecker} and the caching of the
 * {@link DefaultSchemaIncompatibilityChecker}.
 */
class SchemaIncompatibilityCheckerTest {

    private final SchemaIncompatibilityChecker plainChecker = new SchemaIncompatibilityChecker() {
    };

    @Test
    void compatibleSchemasPassTheAssertionSilently() {
        assertDoesNotThrow(() -> plainChecker.assertSchemaCompatibility(
                String.class, AvroTestFixtures.PERSON_V2_WITH_DEFAULT, AvroTestFixtures.PERSON_V1, false));
    }

    @Test
    void incompatibleSchemasFailWithAConversionExceptionListingTheIncompatibilities() {
        ConversionException exception = assertThrows(ConversionException.class,
                () -> plainChecker.assertSchemaCompatibility(
                        String.class, AvroTestFixtures.PERSON_V3_REQUIRED_FIELD, AvroTestFixtures.PERSON_V1, false));

        String message = exception.getMessage();
        assertTrue(message.contains("java.lang.String"), message);
        assertTrue(message.contains("READER_FIELD_MISSING_DEFAULT_VALUE"), message);
        assertTrue(message.contains("Consider to define an upcaster"), message);
        assertFalse(message.contains("reader schema is "), message);
    }

    @Test
    void schemasAreOnlyIncludedInTheMessageWhenRequested() {
        ConversionException exception = assertThrows(ConversionException.class,
                () -> plainChecker.assertSchemaCompatibility(
                        String.class, AvroTestFixtures.PERSON_V3_REQUIRED_FIELD, AvroTestFixtures.PERSON_V1, true));

        assertTrue(exception.getMessage().contains("reader schema is "), exception.getMessage());
        assertTrue(exception.getMessage().contains("writer schema is "), exception.getMessage());
    }

    @Test
    void assertionUsesTheOverriddenCompatibilityCheck() {
        AtomicInteger checks = new AtomicInteger();
        SchemaIncompatibilityChecker permissive = new SchemaIncompatibilityChecker() {
            @Override
            public List<SchemaCompatibility.Incompatibility> checkCompatibility(Schema readerSchema,
                                                                               Schema writerSchema) {
                checks.incrementAndGet();
                return List.of();
            }
        };

        assertDoesNotThrow(() -> permissive.assertSchemaCompatibility(
                String.class, AvroTestFixtures.PERSON_V3_REQUIRED_FIELD, AvroTestFixtures.PERSON_V1, false));
        assertEquals(1, checks.get());
    }

    @Test
    void defaultCheckerCachesTheResultPerReaderAndWriterPair() {
        DefaultSchemaIncompatibilityChecker testSubject = new DefaultSchemaIncompatibilityChecker();

        List<SchemaCompatibility.Incompatibility> first = testSubject.checkCompatibility(
                AvroTestFixtures.PERSON_V3_REQUIRED_FIELD, AvroTestFixtures.PERSON_V1);
        List<SchemaCompatibility.Incompatibility> second = testSubject.checkCompatibility(
                AvroTestFixtures.PERSON_V3_REQUIRED_FIELD, AvroTestFixtures.PERSON_V1);

        assertFalse(first.isEmpty());
        assertSame(first, second);
        assertEquals(1, testSubject.getIncompatibilitiesCache().size());
    }

    @Test
    void defaultCheckerKeepsSeparateCacheEntriesForReaderWriterOrder() {
        DefaultSchemaIncompatibilityChecker testSubject = new DefaultSchemaIncompatibilityChecker();

        List<SchemaCompatibility.Incompatibility> readerNeedsMore = testSubject.checkCompatibility(
                AvroTestFixtures.PERSON_V3_REQUIRED_FIELD, AvroTestFixtures.PERSON_V1);
        List<SchemaCompatibility.Incompatibility> readerNeedsLess = testSubject.checkCompatibility(
                AvroTestFixtures.PERSON_V1, AvroTestFixtures.PERSON_V3_REQUIRED_FIELD);

        assertFalse(readerNeedsMore.isEmpty());
        assertTrue(readerNeedsLess.isEmpty());
        assertEquals(2, testSubject.getIncompatibilitiesCache().size());
    }

    @Test
    void defaultCheckerCacheCanBeClearedAndTheExposedCacheIsACopy() {
        DefaultSchemaIncompatibilityChecker testSubject = new DefaultSchemaIncompatibilityChecker();
        testSubject.checkCompatibility(AvroTestFixtures.PERSON_V1, AvroTestFixtures.PERSON_V1);

        testSubject.getIncompatibilitiesCache().clear();
        assertEquals(1, testSubject.getIncompatibilitiesCache().size());

        testSubject.clear();
        assertTrue(testSubject.getIncompatibilitiesCache().isEmpty());
    }

    @Test
    void defaultCheckerAlsoCachesCompatibleResults() {
        DefaultSchemaIncompatibilityChecker testSubject = new DefaultSchemaIncompatibilityChecker();

        assertTrue(testSubject.checkCompatibility(
                AvroTestFixtures.PERSON_V2_WITH_DEFAULT, AvroTestFixtures.PERSON_V1).isEmpty());

        assertEquals(1, testSubject.getIncompatibilitiesCache().size());
    }
}
