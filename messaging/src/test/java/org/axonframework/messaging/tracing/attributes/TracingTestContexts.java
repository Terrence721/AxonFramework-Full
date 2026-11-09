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

package org.axonframework.messaging.tracing.attributes;

import org.axonframework.common.configuration.Configuration;
import org.axonframework.common.configuration.DefaultComponentRegistry;
import org.axonframework.common.configuration.LifecycleHandler;
import org.axonframework.common.configuration.LifecycleRegistry;
import org.axonframework.messaging.core.ApplicationContext;
import org.axonframework.messaging.core.unitofwork.ProcessingContext;
import org.axonframework.messaging.core.unitofwork.SimpleUnitOfWorkFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Runs code against a real, started {@link ProcessingContext} for the tracing tests.
 */
public final class TracingTestContexts {

    private static final ApplicationContext APPLICATION_CONTEXT = new ApplicationContext() {
        @Override
        public <C> C component(Class<C> type, String name) {
            throw new UnsupportedOperationException();
        }
    };

    private TracingTestContexts() {
    }

    /**
     * Builds a {@link Configuration} from the given registry, without any lifecycle handling.
     */
    public static Configuration build(DefaultComponentRegistry registry) {
        return registry.build(new LifecycleRegistry() {
            @Override
            public LifecycleRegistry registerLifecyclePhaseTimeout(long timeout, TimeUnit timeUnit) {
                return this;
            }

            @Override
            public LifecycleRegistry onStart(int phase, LifecycleHandler startHandler) {
                return this;
            }

            @Override
            public LifecycleRegistry onShutdown(int phase, LifecycleHandler shutdownHandler) {
                return this;
            }
        });
    }

    /**
     * Runs the given {@code action} inside a started unit of work and returns its result once the unit of work
     * completed.
     */
    public static <R> R inStartedContext(Function<ProcessingContext, R> action) {
        return inStartedContext(APPLICATION_CONTEXT, action);
    }

    /**
     * Runs the given {@code action} inside a started unit of work that resolves components from the given
     * {@code applicationContext}, and returns its result once the unit of work completed.
     */
    public static <R> R inStartedContext(ApplicationContext applicationContext,
                                         Function<ProcessingContext, R> action) {
        return new SimpleUnitOfWorkFactory(applicationContext)
                .create()
                .executeWithResult(context -> CompletableFuture.completedFuture(action.apply(context)))
                .join();
    }

    /**
     * Runs the given {@code action} inside a started unit of work that then fails with the given {@code error}, and
     * returns once the unit of work completed.
     */
    public static void inFailingContext(Consumer<ProcessingContext> action, Throwable error) {
        try {
            new SimpleUnitOfWorkFactory(APPLICATION_CONTEXT)
                    .create()
                    .executeWithResult(context -> {
                        action.accept(context);
                        return CompletableFuture.failedFuture(error);
                    })
                    .join();
        } catch (CompletionException expected) {
            // The unit of work is meant to fail
        }
    }
}
