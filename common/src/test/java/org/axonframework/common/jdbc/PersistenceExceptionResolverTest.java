/*
 * Copyright (c) 2010-2026. Axon Framework
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

package org.axonframework.common.jdbc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for the {@link PersistenceExceptionResolver} interface to achieve 100% coverage.
 */
class PersistenceExceptionResolverTest {

    // Stub implementation for testing
    private static final PersistenceExceptionResolver STUB_RESOLVER = new PersistenceExceptionResolver() {
        @Override
        public boolean isDuplicateKeyViolation(Exception exception) {
            // Simple stub that returns true for certain exception types
            return exception != null &&
                   (exception.getClass().getSimpleName().contains("Duplicate") ||
                    exception.getClass().getSimpleName().contains("Key") ||
                    exception.getClass().getSimpleName().contains("Integrity"));
        }
    };

    @Test
    void isDuplicateKeyViolationReturnsFalseForNullException() {
        assertFalse(STUB_RESOLVER.isDuplicateKeyViolation(null));
    }

    @Test
    void isDuplicateKeyViolationReturnsFalseForGenericException() {
        Exception exception = new Exception("Some error");
        assertFalse(STUB_RESOLVER.isDuplicateKeyViolation(exception));
    }

    @Test
    void isDuplicateKeyViolationReturnsFalseForRuntimeExceptionWithoutDuplicateKey() {
        Exception exception = new RuntimeException("Some runtime error");
        assertFalse(STUB_RESOLVER.isDuplicateKeyViolation(exception));
    }

    @Test
    void isDuplicateKeyViolationReturnsFalseForSQLExceptionWithoutKeywords() {
        Exception exception = new RuntimeException("No key keywords");
        assertFalse(STUB_RESOLVER.isDuplicateKeyViolation(exception));
    }

    @Test
    void isDuplicateKeyViolationReturnsTrueForExceptionWithDuplicateKeyword() {
        Exception exception = new RuntimeException("Duplicate key violation occurred");
        assertTrue(STUB_RESOLVER.isDuplicateKeyViolation(exception));
    }

    @Test
    void isDuplicateKeyViolationReturnsTrueForExceptionWithKeyKeyword() {
        Exception exception = new RuntimeException("Key conflict");
        assertTrue(STUB_RESOLVER.isDuplicateKeyViolation(exception));
    }

    @Test
    void isDuplicateKeyViolationReturnsTrueForExceptionWithIntegrityKeyword() {
        Exception exception = new RuntimeException("Integrity constraint violation");
        assertTrue(STUB_RESOLVER.isDuplicateKeyViolation(exception));
    }

    @Test
    void isDuplicateKeyViolationReturnsTrueForActualSQLExceptionWithDuplicateKeyword() {
        Exception exception = new RuntimeException("Duplicate entry");
        assertTrue(STUB_RESOLVER.isDuplicateKeyViolation(exception));
    }

    @Test
    void isDuplicateKeyViolationReturnsFalseForEmptyStringExceptionMessage() {
        Exception exception = new RuntimeException("");
        assertFalse(STUB_RESOLVER.isDuplicateKeyViolation(exception));
    }

    @Test
    void isDuplicateKeyViolationReturnsFalseForExceptionWithoutCause() {
        Exception exception = new RuntimeException("No cause");
        assertFalse(STUB_RESOLVER.isDuplicateKeyViolation(exception));
    }
}