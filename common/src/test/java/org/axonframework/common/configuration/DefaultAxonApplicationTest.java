package org.axonframework.common.configuration;

import org.axonframework.common.lifecycle.LifecycleHandlerInvocationException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultAxonApplicationTest {

    @Test
    void buildIsIdempotent() {
        DefaultAxonApplication application = new DefaultAxonApplication();

        assertSame(application.build(), application.build());
    }

    @Test
    void lifecycleRegistrationIsRejectedAfterBuild() {
        DefaultAxonApplication application = new DefaultAxonApplication();
        application.build();

        assertThrows(IllegalStateException.class,
                () -> application.onStart(0, (LifecycleHandler) configuration -> CompletableFuture.completedFuture(null)));
        assertThrows(IllegalStateException.class,
                () -> application.onShutdown(0, (LifecycleHandler) configuration -> CompletableFuture.completedFuture(null)));
    }

    @Test
    void invalidLifecycleTimeoutIsRejected() {
        DefaultAxonApplication application = new DefaultAxonApplication();

        assertThrows(IllegalArgumentException.class,
                () -> application.registerLifecyclePhaseTimeout(0, TimeUnit.SECONDS));
        assertThrows(NullPointerException.class,
                () -> application.registerLifecyclePhaseTimeout(1, null));
    }

    @Test
    void startAndShutdownRunSuccessfully() {
        DefaultAxonApplication application = new DefaultAxonApplication();
        application.onStart(0, (LifecycleHandler) configuration -> CompletableFuture.completedFuture(null));
        application.onShutdown(0, (LifecycleHandler) configuration -> CompletableFuture.completedFuture(null));

        var configuration = application.start();
        configuration.shutdown();
    }

    @Test
    void failingStartHandlerThrowsLifecycleException() {
        DefaultAxonApplication application = new DefaultAxonApplication();
        application.onStart(0, (LifecycleHandler) configuration ->
                CompletableFuture.failedFuture(new IllegalStateException("boom")));

        assertThrows(LifecycleHandlerInvocationException.class, application::start);
    }
}