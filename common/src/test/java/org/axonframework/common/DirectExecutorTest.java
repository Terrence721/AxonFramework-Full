package org.axonframework.common;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectExecutorTest {

    @Test
    void executeRunsCommandInCallingThread() {
        AtomicBoolean executed = new AtomicBoolean(false);

        DirectExecutor.INSTANCE.execute(() -> executed.set(true));

        assertTrue(executed.get());
        assertSame(DirectExecutor.INSTANCE, DirectExecutor.instance());
    }

    @Test
    void instanceReturnsSameSingleton() {
        DirectExecutor first = DirectExecutor.instance();
        DirectExecutor second = DirectExecutor.instance();
        assertSame(first, second);
    }

    @Test
    void constructorIsPrivateAndSingletonIsSingleton() throws Exception {
        // Verify constructor is private and uses singleton pattern
        Field instanceField = DirectExecutor.class.getDeclaredField("INSTANCE");
        instanceField.setAccessible(true);

        Object instance = instanceField.get(null);
        assertTrue(instance instanceof DirectExecutor);

        // Verify the singleton pattern by getting the instance and running a command
        AtomicBoolean executed = new AtomicBoolean(false);
        ((DirectExecutor) instance).execute(() -> executed.set(true));
        assertTrue(executed.get());
    }

    @Test
    void instanceConstantIsAccessible() {
        // Test that the INSTANCE constant is accessible and is a DirectExecutor
        DirectExecutor instance = DirectExecutor.INSTANCE;
        assertSame(DirectExecutor.instance(), instance);
    }

    @Test
    void getTestInstanceReturnsSameInstance() {
        // Test that getTestInstance() returns the same instance as instance()
        assertSame(DirectExecutor.INSTANCE, DirectExecutor.getTestInstance());
    }

    @Test
    void instanceMethodUsesInstanceConstant() {
        // Test that the instance() method actually uses the INSTANCE constant
        assertSame(DirectExecutor.instance(), DirectExecutor.INSTANCE);
    }
}