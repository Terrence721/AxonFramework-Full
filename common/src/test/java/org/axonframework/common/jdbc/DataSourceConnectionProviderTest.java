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

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test class validating the {@link DataSourceConnectionProvider}.
 */
class DataSourceConnectionProviderTest {

    private static Connection stubConnection() {
        return (Connection) Proxy.newProxyInstance(
                DataSourceConnectionProviderTest.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    /**
     * A {@link DataSource} stub whose only supported operation is {@link DataSource#getConnection()}, delegating to
     * the given supplier. Counts how often a connection was requested.
     */
    private static DataSource dataSource(ThrowingSupplier supplier, AtomicInteger requests) {
        return (DataSource) Proxy.newProxyInstance(
                DataSourceConnectionProviderTest.class.getClassLoader(),
                new Class<?>[]{DataSource.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getConnection") && method.getParameterCount() == 0) {
                        requests.incrementAndGet();
                        return supplier.get();
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    @FunctionalInterface
    private interface ThrowingSupplier {

        Connection get() throws SQLException;
    }

    @Test
    void constructorRejectsNullDataSource() {
        NullPointerException exception =
                assertThrows(NullPointerException.class, () -> new DataSourceConnectionProvider(null));

        assertEquals("dataSource may not be null", exception.getMessage());
    }

    @Test
    void getConnectionReturnsConnectionFromDataSource() throws SQLException {
        Connection connection = stubConnection();
        AtomicInteger requests = new AtomicInteger();
        DataSourceConnectionProvider testSubject =
                new DataSourceConnectionProvider(dataSource(() -> connection, requests));

        assertSame(connection, testSubject.getConnection());
        assertEquals(1, requests.get());
    }

    @Test
    void getConnectionAsksDataSourceOnEveryInvocation() throws SQLException {
        AtomicInteger requests = new AtomicInteger();
        DataSourceConnectionProvider testSubject =
                new DataSourceConnectionProvider(dataSource(DataSourceConnectionProviderTest::stubConnection, requests));

        Connection first = testSubject.getConnection();
        Connection second = testSubject.getConnection();

        assertNotSame(first, second);
        assertEquals(2, requests.get());
    }

    @Test
    void getConnectionPropagatesSqlExceptionFromDataSource() {
        SQLException failure = new SQLException("pool exhausted");
        DataSourceConnectionProvider testSubject = new DataSourceConnectionProvider(dataSource(() -> {
            throw failure;
        }, new AtomicInteger()));

        SQLException thrown = assertThrows(SQLException.class, testSubject::getConnection);

        assertSame(failure, thrown);
    }
}
