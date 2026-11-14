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

package org.axonframework.common.configuration;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for {@link ComponentLifecycleHandler} interface to achieve 100% coverage.
 */
class ComponentLifecycleHandlerTest {

    @Test
    void runWithSynchronousOperation() {
        ComponentLifecycleHandler<String> handler = (configuration, component) ->
                CompletableFuture.completedFuture("synchronous-result");

        CompletableFuture<?> future = handler.run(null, "test-component");

        assertTrue(future.isDone());
        assertEquals("synchronous-result", future.join());
    }

    @Test
    void runWithAsynchronousOperation() {
        CompletableFuture<String> future = new CompletableFuture<>();
        ComponentLifecycleHandler<String> handler = (configuration, component) -> future;

        CompletableFuture<?> result = handler.run(null, "test-component");

        assertFalse(result.isDone());
        future.complete("async-result");
        assertTrue(result.isDone());
        assertEquals("async-result", result.join());
    }

    @Test
    void runWithExceptionPropagation() {
        CompletableFuture<String> future = new CompletableFuture<>();
        ComponentLifecycleHandler<String> handler = (configuration, component) -> future;

        CompletableFuture<?> result = handler.run(null, "test-component");

        assertFalse(result.isDone());
        future.completeExceptionally(new RuntimeException("test-failure"));

        assertTrue(result.isDone());
        assertTrue(result.isCompletedExceptionally());
    }

    @Test
    void runWithConfigurationParameter() {
        ComponentLifecycleHandler<String> handler = (configuration, component) -> {
            assertEquals("test-component", component);
            return CompletableFuture.completedFuture("received-params");
        };

        CompletableFuture<?> future = handler.run(null, "test-component");

        assertTrue(future.isDone());
        assertEquals("received-params", future.join());
    }

    @Test
    void runWithNullParameters() {
        ComponentLifecycleHandler<Object> handler = (configuration, component) -> {
            assertNull(configuration);
            assertNull(component);
            return CompletableFuture.completedFuture("null-params");
        };

        CompletableFuture<?> future = handler.run(null, null);

        assertTrue(future.isDone());
        assertEquals("null-params", future.join());
    }

    @Test
    void runWithComplexComponentType() {
        class CustomComponent {
            private final String value;

            CustomComponent(String value) {
                this.value = value;
            }

            @Override
            public String toString() {
                return "CustomComponent{" + value + "}";
            }
        }

        CustomComponent component = new CustomComponent("test");
        ComponentLifecycleHandler<CustomComponent> handler = (configuration, comp) ->
                CompletableFuture.completedFuture(comp.toString());

        CompletableFuture<?> future = handler.run(null, component);

        assertTrue(future.isDone());
        assertEquals("CustomComponent{test}", future.join());
    }
}