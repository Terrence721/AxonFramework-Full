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

package org.axonframework.common.jdbc;

import org.axonframework.common.AxonTransientException;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Test class validating the {@link JdbcException}.
 */
class JdbcExceptionTest {

    @Test
    void retainsMessageAndCause() {
        SQLException cause = new SQLException("connection reset");

        JdbcException testSubject = new JdbcException("query failed", cause);

        assertEquals("query failed", testSubject.getMessage());
        assertSame(cause, testSubject.getCause());
    }

    @Test
    void acceptsNullCause() {
        JdbcException testSubject = new JdbcException("query failed", null);

        assertEquals("query failed", testSubject.getMessage());
        assertNull(testSubject.getCause());
    }

    @Test
    void isTransientAxonException() {
        assertInstanceOf(AxonTransientException.class, new JdbcException("query failed", null));
    }
}
