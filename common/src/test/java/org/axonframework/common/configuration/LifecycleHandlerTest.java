package org.axonframework.common.configuration;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LifecycleHandlerTest {

    @Test
    void runReturnsCompletedFuture() {
        LifecycleHandler handler = configuration -> CompletableFuture.completedFuture("done");

        assertEquals("done", handler.run(null).join());
    }
}