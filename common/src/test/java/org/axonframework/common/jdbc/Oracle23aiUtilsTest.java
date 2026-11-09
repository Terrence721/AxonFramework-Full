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

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link Oracle23aiUtils}.
 */
class Oracle23aiUtilsTest {

    private final List<String> executed = new ArrayList<>();
    private final List<String> prepared = new ArrayList<>();
    private boolean statementClosed;
    private String failOn;

    private Connection connection(PreparedStatement preparedStatement) {
        Statement statement = (Statement) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{Statement.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "execute":
                            String sql = (String) args[0];
                            if (sql.equals(failOn)) {
                                throw new SQLException("ORA-00942");
                            }
                            executed.add(sql);
                            return true;
                        case "close":
                            statementClosed = true;
                            return null;
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                }
        );
        return (Connection) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "createStatement":
                            return statement;
                        case "prepareStatement":
                            prepared.add((String) args[0]);
                            return preparedStatement;
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                }
        );
    }

    @Test
    void addAutoIncrementDropsAndRecreatesColumnAsIdentityInThatOrder() throws SQLException {
        Oracle23aiUtils.addAutoIncrement(connection(null), "events", "global_index");

        assertEquals(List.of(
                "ALTER TABLE events DROP COLUMN global_index",
                "ALTER TABLE events ADD (global_index NUMBER GENERATED AS IDENTITY "
                        + "(START WITH 1 INCREMENT BY 1 NOCYCLE))"
        ), executed);
        assertTrue(statementClosed);
    }

    @Test
    void addAutoIncrementClosesStatementAndStopsWhenDropFails() {
        failOn = "ALTER TABLE events DROP COLUMN global_index";

        assertThrows(SQLException.class,
                     () -> Oracle23aiUtils.addAutoIncrement(connection(null), "events", "global_index"));

        assertTrue(executed.isEmpty());
        assertTrue(statementClosed);
    }

    @Test
    void createNullStatementPreparesNoOpQueryAgainstDual() throws SQLException {
        PreparedStatement expected = (PreparedStatement) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{PreparedStatement.class},
                (proxy, method, args) -> null
        );

        PreparedStatement result = Oracle23aiUtils.createNullStatement(connection(expected));

        assertSame(expected, result);
        assertEquals(List.of("select 1 from dual"), prepared);
    }
}
