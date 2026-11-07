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
 * See the License for the specific language governing permissions and limitations
 * under the License.
 */

package org.axonframework.common.jdbc;

import org.junit.jupiter.api.Test;

import java.sql.SQLIntegrityConstraintViolationException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcSQLErrorCodesResolverTest {

    @Test
    void returnsTrueWhenConstraintViolationNested() {
        RuntimeException outer = new RuntimeException(
                new SQLIntegrityConstraintViolationException("duplicate")
        );

        assertTrue(new JdbcSQLErrorCodesResolver().isDuplicateKeyViolation(outer));
    }

    @Test
    void returnsFalseForOtherException() {
        assertFalse(new JdbcSQLErrorCodesResolver().isDuplicateKeyViolation(new IllegalArgumentException()));
    }
}
