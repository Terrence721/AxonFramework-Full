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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Utility class with some specific helpers to get certain features to work with Oracle 23ai.
 */
public class Oracle23aiUtils {

    private Oracle23aiUtils() {
    }

    /**
     * Oracle 23ai supports native identity columns, so auto-increment behavior no longer needs the
     * sequence-and-trigger workaround earlier Oracle versions required. This method converts the given
     * {@code columnName} on the given {@code tableName} into a {@code GENERATED AS IDENTITY} column.
     * <p>
     * <b>Destructive:</b> Oracle has no way to convert an existing plain column directly into an identity
     * column &mdash; {@code ALTER TABLE ... MODIFY ... GENERATED AS IDENTITY} only adjusts a column that is
     * <em>already</em> an identity column (confirmed against a real Oracle 23ai instance: it fails with
     * {@code ORA-30673} otherwise). The only way to add identity behavior is to drop the column and
     * re-add it as identity, which discards any data already in it. Only call this on a column with no
     * data worth keeping, typically right after creating the table.
     *
     * @param connection The connection to the database that will be used to execute the query
     * @param tableName  The name of the table that contains the column that should be automatically incremented
     * @param columnName The name of the column that should be automatically incremented. Any existing data
     *                   in this column is discarded.
     * @throws SQLException if the identity column cannot be created
     */
    public static void addAutoIncrement(Connection connection, String tableName, String columnName) throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("ALTER TABLE " + tableName + " DROP COLUMN " + columnName);
            st.execute("ALTER TABLE " + tableName + " ADD (" + columnName +
                    " NUMBER GENERATED AS IDENTITY (START WITH 1 INCREMENT BY 1 NOCYCLE))");
        }
    }

    /**
     * Creates a prepared statement that acts as a null object.
     *
     * @param connection The connection that is used to create the prepared statement
     * @return PreparedStatement
     * @throws SQLException if the null statement cannot be created
     */
    public static PreparedStatement createNullStatement(Connection connection) throws SQLException {
        return connection.prepareStatement("select 1 from dual");
    }
}
