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
 * See the License for the specific language governing permissions and limitations
 * under the License.
 */

package org.axonframework.common;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExceptionUtilsTest {

    @Test
    void findExceptionByClassReturnsMatchingCause() {
        RuntimeException cause = new RuntimeException("cause");
        RuntimeException outer = new RuntimeException("outer", cause);

        Optional<RuntimeException> result = ExceptionUtils.findException(outer, RuntimeException.class);

        assertTrue(result.isPresent());
        assertEquals("outer", result.get().getMessage());
    }

    @Test
    void findExceptionByPredicateReturnsMatchingException() {
        RuntimeException cause = new RuntimeException("cause");
        RuntimeException outer = new RuntimeException("outer", cause);

        Optional<Throwable> result = ExceptionUtils.findException(outer, t -> t.getMessage().contains("cause"));

        assertTrue(result.isPresent());
        assertEquals("cause", result.get().getMessage());
    }

    @Test
    void findExceptionWithPredicateUsesInstanceofWorks() {
        // Use the overloaded method that takes Predicate<Throwable>
        Optional<Throwable> result = ExceptionUtils.findException(
            new RuntimeException("test"),
            t -> t.getMessage().equals("test")
        );

        assertTrue(result.isPresent());
        assertEquals("test", result.get().getMessage());
    }

    @Test
    void findExceptionReturnsEmptyWhenNoMatch() {
        IOException ioException = new IOException("IO error");

        // Use the method that takes Class<T> - this returns Optional<T>
        Optional<RuntimeException> result = ExceptionUtils.findException(ioException, RuntimeException.class);

        assertFalse(result.isPresent());
    }

    @Test
    void findExceptionHandlesNullInput() {
        // Use the overloaded method that returns Optional<Throwable>
        Optional<RuntimeException> result = ExceptionUtils.findException(null, RuntimeException.class);

        assertFalse(result.isPresent());
    }

    @Test
    void findExceptionWithClassPredicateUsesInstanceof() {
        RuntimeException cause = new RuntimeException("cause");
        RuntimeException outer = new RuntimeException("outer", cause);

        // Test with a more specific class that the cause is an instance of
        Optional<RuntimeException> result = ExceptionUtils.findException(outer, RuntimeException.class);

        assertTrue(result.isPresent());
        assertTrue(result.get() instanceof RuntimeException);
    }

    @Test
    void isExplicitlyNonTransientReturnsTrueForWrappedException() {
        AxonConfigurationException root = new AxonConfigurationException("root");
        RuntimeException outer = new RuntimeException("outer", root);

        assertTrue(ExceptionUtils.isExplicitlyNonTransient(outer));
    }

    @Test
    void isExplicitlyNonTransientReturnsFalseForNull() {
        assertFalse(ExceptionUtils.isExplicitlyNonTransient(null));
    }

    @Test
    void isExplicitlyNonTransientReturnsFalseForNonAxonException() {
        IllegalArgumentException rawException = new IllegalArgumentException("bad argument");

        assertFalse(ExceptionUtils.isExplicitlyNonTransient(rawException));
    }

    @Test
    void isExplicitlyNonTransientReturnsFalseForRuntimeException() {
        RuntimeException runtimeEx = new RuntimeException("runtime problem");

        assertFalse(ExceptionUtils.isExplicitlyNonTransient(runtimeEx));
    }

    @Test
    void isExplicitlyNonTransientRecursivelyChecksCauses() {
        // Create a chain: RuntimeException -> AxonConfigurationException -> RuntimeException
        // AxonNonTransientException is abstract, use AxonConfigurationException (concrete subclass)
        RuntimeException inner = new RuntimeException("inner cause");
        AxonConfigurationException middle = new AxonConfigurationException("middle cause", inner);
        RuntimeException outer = new RuntimeException("outer cause", middle);

        assertTrue(ExceptionUtils.isExplicitlyNonTransient(outer));
    }

    @Test
    void isExplicitlyNonTransientHandlesDeepCauseChain() {
        // Deep chain: IOException -> RuntimeException -> AxonConfigurationException
        IOException io = new IOException("IO problem");
        RuntimeException re = new RuntimeException("runtime", io);
        AxonConfigurationException ante = new AxonConfigurationException("axon", re);

        assertTrue(ExceptionUtils.isExplicitlyNonTransient(ante));
    }

    @Test
    void findExceptionMethodHandlesAllCauseLevels() {
        // Test that findException with predicate traverses the full cause chain
        IOException io = new IOException("IO problem");
        RuntimeException re = new RuntimeException("runtime", io);
        IllegalStateException ise = new IllegalStateException("state", re);

        Optional<Throwable> result = ExceptionUtils.findException(
            ise,
            t -> t instanceof IOException
        );

        assertTrue(result.isPresent());
        assertTrue(result.get() instanceof IOException);
    }

    @Test
    void findExceptionWithClassHandlesAllCauseLevels() {
        // Test that findException with class traverses the full cause chain
        IOException io = new IOException("IO problem");
        RuntimeException re = new RuntimeException("runtime", io);
        IllegalStateException ise = new IllegalStateException("state", re);

        Optional<IOException> result = ExceptionUtils.findException(ise, IOException.class);

        assertTrue(result.isPresent());
        assertTrue(result.get() instanceof IOException);
    }
}
