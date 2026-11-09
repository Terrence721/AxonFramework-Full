package org.axonframework.common.jdbc;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConnectionWrapperFactoryTest {

    interface Wrapper {
        String foo();
    }

    @Test
    void wrapWithWrapperInterface() throws java.sql.SQLException {
        Connection connection = fakeConnection(false);
        ConnectionWrapperFactory.ConnectionCloseHandler handler = new ConnectionWrapperFactory.NoOpCloseHandler();
        Wrapper wrapperHandler = () -> "bar";

        Connection wrapped = ConnectionWrapperFactory.wrap(connection, Wrapper.class, wrapperHandler, handler);

        assertEquals("bar", ((Wrapper) wrapped).foo());
        wrapped.close();
        wrapped.commit();
        wrapped.getAutoCommit(); // falls through to underlying connection
        assertEquals(connection.hashCode(), wrapped.hashCode());
        assertNotEquals(wrapped, new Object());
        assertEquals(wrapped, wrapped);
    }

    @Test
    void wrapWithoutWrapperInterface() throws java.sql.SQLException {
        Connection connection = fakeConnection(true);
        ConnectionWrapperFactory.ConnectionCloseHandler handler = new ConnectionWrapperFactory.NoOpCloseHandler();

        Connection wrapped = ConnectionWrapperFactory.wrap(connection, handler);

        assertTrue(wrapped.getAutoCommit());
        assertEquals(connection.hashCode(), wrapped.hashCode());
        assertEquals(wrapped, wrapped);
        assertNotEquals(wrapped, new Object());
        wrapped.close();
        wrapped.commit();
    }

    @Test
    void invokeUnwrapsNestedException() {
        Connection connection = fakeConnection(false);
        ConnectionWrapperFactory.ConnectionCloseHandler handler = new ConnectionWrapperFactory.NoOpCloseHandler();
        Wrapper wrapperHandler = () -> {
            throw new IllegalStateException("boom");
        };

        Connection wrapped = ConnectionWrapperFactory.wrap(connection, Wrapper.class, wrapperHandler, handler);

        assertThrows(IllegalStateException.class, () -> ((Wrapper) wrapped).foo());
    }

    private static Connection fakeConnection(boolean autoCommitValue) {
        return (Connection) Proxy.newProxyInstance(
                ConnectionWrapperFactoryTest.class.getClassLoader(),
                new Class[]{Connection.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getAutoCommit":
                            return autoCommitValue;
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "equals":
                            return proxy == args[0];
                        case "toString":
                            return "FakeConnection@" + System.identityHashCode(proxy);
                        default:
                            return null;
                    }
                });
    }
}