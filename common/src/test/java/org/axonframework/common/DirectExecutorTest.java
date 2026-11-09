package org.axonframework.common;

import org.junit.jupiter.api.Test;

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
}