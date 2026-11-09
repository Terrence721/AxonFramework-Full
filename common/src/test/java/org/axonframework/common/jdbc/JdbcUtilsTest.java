package org.axonframework.common.jdbc;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JdbcUtilsTest {

    private static final java.util.function.Function<SQLException, RuntimeException> ERROR_HANDLER =
            e -> new RuntimeException(e);

    @Test
    void executeQuerySuccess() throws SQLException {
        JdbcTestDoubles.ResultSetState resultSetState = new JdbcTestDoubles.ResultSetState();
        ResultSet resultSet = JdbcTestDoubles.fakeResultSet(resultSetState);
        JdbcTestDoubles.PreparedStatementState statementState = new JdbcTestDoubles.PreparedStatementState();
        statementState.queryResult = resultSet;
        PreparedStatement statement = JdbcTestDoubles.fakePreparedStatement(statementState);
        Connection connection = JdbcTestDoubles.fakeConnection(statement);

        String result = JdbcUtils.executeQuery(
                connection,
                c -> statement,
                rs -> "result",
                ERROR_HANDLER,
                true
        );

        assertEquals("result", result);
    }

    @Test
    void executeQueryExecuteThrows() throws SQLException {
        JdbcTestDoubles.PreparedStatementState statementState = new JdbcTestDoubles.PreparedStatementState();
        statementState.queryException = new SQLException("boom");
        PreparedStatement statement = JdbcTestDoubles.fakePreparedStatement(statementState);
        Connection connection = JdbcTestDoubles.fakeConnection(statement);

        assertThrows(RuntimeException.class, () -> JdbcUtils.executeQuery(
                connection,
                c -> statement,
                rs -> "result",
                ERROR_HANDLER,
                false
        ));
    }

    @Test
    void executeQueryConverterThrows() throws SQLException {
        JdbcTestDoubles.ResultSetState resultSetState = new JdbcTestDoubles.ResultSetState();
        ResultSet resultSet = JdbcTestDoubles.fakeResultSet(resultSetState);
        JdbcTestDoubles.PreparedStatementState statementState = new JdbcTestDoubles.PreparedStatementState();
        statementState.queryResult = resultSet;
        PreparedStatement statement = JdbcTestDoubles.fakePreparedStatement(statementState);
        Connection connection = JdbcTestDoubles.fakeConnection(statement);

        assertThrows(RuntimeException.class, () -> JdbcUtils.executeQuery(
                connection,
                c -> statement,
                rs -> {
                    throw new SQLException("convert");
                },
                ERROR_HANDLER,
                false
        ));
    }

    @Test
    void executeUpdate() throws SQLException {
        JdbcTestDoubles.PreparedStatementState statementState = new JdbcTestDoubles.PreparedStatementState();
        statementState.updateResult = 3;
        PreparedStatement statement = JdbcTestDoubles.fakePreparedStatement(statementState);
        Connection connection = JdbcTestDoubles.fakeConnection(statement);

        int result = JdbcUtils.executeUpdate(connection, c -> statement, ERROR_HANDLER);

        assertEquals(3, result);
    }

    @Test
    void executeUpdateThrows() throws SQLException {
        JdbcTestDoubles.PreparedStatementState statementState = new JdbcTestDoubles.PreparedStatementState();
        statementState.updateException = new SQLException("boom");
        PreparedStatement statement = JdbcTestDoubles.fakePreparedStatement(statementState);
        Connection connection = JdbcTestDoubles.fakeConnection(statement);

        assertThrows(RuntimeException.class, () -> JdbcUtils.executeUpdate(connection, c -> statement, ERROR_HANDLER));
    }

    @Test
    void executeUpdates() throws SQLException {
        JdbcTestDoubles.PreparedStatementState statementState = new JdbcTestDoubles.PreparedStatementState();
        statementState.updateResult = 2;
        PreparedStatement statement = JdbcTestDoubles.fakePreparedStatement(statementState);
        Connection connection = JdbcTestDoubles.fakeConnection(statement);

        int[] result = JdbcUtils.executeUpdates(connection, e -> {
        }, c -> statement);

        assertArrayEquals(new int[] { 2 }, result);
    }

    @Test
    void executeBatch() throws SQLException {
        JdbcTestDoubles.PreparedStatementState statementState = new JdbcTestDoubles.PreparedStatementState();
        statementState.batchResult = new int[] { 1, 2 };
        PreparedStatement statement = JdbcTestDoubles.fakePreparedStatement(statementState);
        Connection connection = JdbcTestDoubles.fakeConnection(statement);

        int[] result = JdbcUtils.executeBatch(connection, c -> statement, e -> {
        });

        assertArrayEquals(new int[] { 1, 2 }, result);
    }

    @Test
    void executeBatchThrows() throws SQLException {
        JdbcTestDoubles.PreparedStatementState statementState = new JdbcTestDoubles.PreparedStatementState();
        statementState.batchException = new SQLException("boom");
        PreparedStatement statement = JdbcTestDoubles.fakePreparedStatement(statementState);
        Connection connection = JdbcTestDoubles.fakeConnection(statement);

        int[] result = JdbcUtils.executeBatch(connection, c -> statement, e -> {
        });

        assertEquals(0, result.length);
    }

    @Test
    void listResults() throws SQLException {
        JdbcTestDoubles.ResultSetState resultSetState = new JdbcTestDoubles.ResultSetState();
        resultSetState.nextTrueCount = 2;
        ResultSet resultSet = JdbcTestDoubles.fakeResultSet(resultSetState);

        JdbcUtils.SqlResultConverter<Integer> single = rs -> 1;
        java.util.List<Integer> result = JdbcUtils.listResults(single).apply(resultSet);

        assertEquals(2, result.size());
    }

    @Test
    void closeQuietlyNoOps() throws SQLException {
        JdbcUtils.closeQuietly((ResultSet) null);
        JdbcUtils.closeQuietly((PreparedStatement) null);
        JdbcUtils.closeQuietly((Connection) null);

        JdbcTestDoubles.ResultSetState resultSetState = new JdbcTestDoubles.ResultSetState();
        resultSetState.closeThrows = true;
        ResultSet rs = JdbcTestDoubles.fakeResultSet(resultSetState);
        JdbcUtils.closeQuietly(rs);
    }

    @Test
    void extractAndNext() throws SQLException {
        JdbcTestDoubles.ResultSetState resultSetState = new JdbcTestDoubles.ResultSetState();
        resultSetState.nextTrueCount = 1;
        resultSetState.objectValue = "value";
        resultSetState.wasNull = false;
        ResultSet resultSet = JdbcTestDoubles.fakeResultSet(resultSetState);

        assertEquals("value", JdbcUtils.extract(resultSet, 1, String.class));
        assertEquals("value", JdbcUtils.extract(resultSet, 1, String.class, "default"));
    }

    @Test
    void createSqlStatementThrows() {
        Connection connection = JdbcTestDoubles.fakeConnection(null);
        assertThrows(JdbcException.class, () -> JdbcUtils.executeUpdate(connection, c -> {
            throw new SQLException("create");
        }, ERROR_HANDLER));
    }

    static final class JdbcTestDoubles {
        static final class PreparedStatementState {
            ResultSet queryResult;
            SQLException queryException;
            int updateResult;
            SQLException updateException;
            int[] batchResult = new int[0];
            SQLException batchException;
        }

        static final class ResultSetState {
            int nextTrueCount;
            int nextCalls;
            Object objectValue;
            boolean wasNull;
            boolean closeThrows;
        }

        static Connection fakeConnection(PreparedStatement statement) {
            return (Connection) Proxy.newProxyInstance(
                    JdbcUtilsTest.class.getClassLoader(),
                    new Class[]{Connection.class},
                    (proxy, method, args) -> {
                        if ("prepareStatement".equals(method.getName())) {
                            return statement;
                        }
                        if ("close".equals(method.getName())) {
                            return null;
                        }
                        if ("hashCode".equals(method.getName())) {
                            return System.identityHashCode(proxy);
                        }
                        if ("equals".equals(method.getName()) && args != null && args.length == 1) {
                            return proxy == args[0];
                        }
                        if ("toString".equals(method.getName())) {
                            return "FakeConnection@" + System.identityHashCode(proxy);
                        }
                        return defaultValue(method.getReturnType());
                    });
        }

        static PreparedStatement fakePreparedStatement(PreparedStatementState state) {
            return (PreparedStatement) Proxy.newProxyInstance(
                    JdbcUtilsTest.class.getClassLoader(),
                    new Class[]{PreparedStatement.class},
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "executeQuery":
                                if (state.queryException != null) {
                                    throw state.queryException;
                                }
                                return state.queryResult;
                            case "executeUpdate":
                                if (state.updateException != null) {
                                    throw state.updateException;
                                }
                                return state.updateResult;
                            case "executeBatch":
                                if (state.batchException != null) {
                                    throw state.batchException;
                                }
                                return state.batchResult;
                            case "close":
                                return null;
                            default:
                                break;
                        }
                        if ("hashCode".equals(method.getName())) {
                            return System.identityHashCode(proxy);
                        }
                        if ("equals".equals(method.getName()) && args != null && args.length == 1) {
                            return proxy == args[0];
                        }
                        if ("toString".equals(method.getName())) {
                            return "FakePreparedStatement@" + System.identityHashCode(proxy);
                        }
                        return defaultValue(method.getReturnType());
                    });
        }

        static ResultSet fakeResultSet(ResultSetState state) {
            return (ResultSet) Proxy.newProxyInstance(
                    JdbcUtilsTest.class.getClassLoader(),
                    new Class[]{ResultSet.class},
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "next":
                                state.nextCalls++;
                                return state.nextCalls <= state.nextTrueCount;
                            case "getObject":
                                return state.objectValue;
                            case "wasNull":
                                return state.wasNull;
                            case "close":
                                if (state.closeThrows) {
                                    throw new SQLException("boom");
                                }
                                return null;
                            default:
                                break;
                        }
                        if ("hashCode".equals(method.getName())) {
                            return System.identityHashCode(proxy);
                        }
                        if ("equals".equals(method.getName()) && args != null && args.length == 1) {
                            return proxy == args[0];
                        }
                        if ("toString".equals(method.getName())) {
                            return "FakeResultSet@" + System.identityHashCode(proxy);
                        }
                        return defaultValue(method.getReturnType());
                    });
        }

        private static Object defaultValue(Class<?> returnType) {
            if (returnType == boolean.class) {
                return false;
            }
            if (returnType == int.class) {
                return 0;
            }
            if (returnType == long.class) {
                return 0L;
            }
            if (returnType == double.class) {
                return 0.0;
            }
            if (returnType == float.class) {
                return 0.0f;
            }
            if (returnType == char.class) {
                return (char) 0;
            }
            if (returnType == short.class) {
                return (short) 0;
            }
            if (returnType == byte.class) {
                return (byte) 0;
            }
            return null;
        }
    }
}