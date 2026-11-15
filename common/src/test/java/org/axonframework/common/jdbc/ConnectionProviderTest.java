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

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test class for {@link ConnectionProvider} interface to achieve 100% coverage.
 */
class ConnectionProviderTest {

    private static Connection stubConnection() {
        return (Connection) Proxy.newProxyInstance(
                ConnectionProviderTest.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    @Test
    void getConnectionReturnsConnectionFromImplementation() throws SQLException {
        StubConnectionProvider testSubject = new StubConnectionProvider();
        Connection expectedConnection = testSubject.createConnection();

        Connection actualConnection = testSubject.getConnection();

        assertNotNull(actualConnection);
        assertSame(expectedConnection, actualConnection);
    }

    @Test
    void getConnectionPropagatesSQLException() {
        FailingConnectionProvider testSubject = new FailingConnectionProvider();

        SQLException exception = assertThrows(SQLException.class, testSubject::getConnection);

        assertNotNull(exception);
    }

    private static class StubConnectionProvider implements ConnectionProvider {

        private final Connection connection = stubConnection();

        @Override
        public Connection getConnection() throws SQLException {
            return connection;
        }

        private Connection createConnection() {
            return connection;
        }
    }

    private static class FailingConnectionProvider implements ConnectionProvider {

        @Override
        public Connection getConnection() throws SQLException {
            throw new SQLException("Database connection failed");
        }
    }
}