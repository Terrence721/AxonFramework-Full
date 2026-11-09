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

package org.axonframework.update.configuration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link CommandLineUsagePropertyProvider}, the
 * {@link EnvironmentVariableUsagePropertyProvider} and {@link UsagePropertyProvider#parseNullableBoolean(String)}.
 */
class SystemAndEnvironmentUsagePropertyProviderTest {

    private String originalDisabled;
    private String originalUrl;

    @BeforeEach
    void saveSystemProperties() {
        originalDisabled = System.getProperty(CommandLineUsagePropertyProvider.DISABLED_KEY);
        originalUrl = System.getProperty(CommandLineUsagePropertyProvider.URL_KEY);
        System.clearProperty(CommandLineUsagePropertyProvider.DISABLED_KEY);
        System.clearProperty(CommandLineUsagePropertyProvider.URL_KEY);
    }

    @AfterEach
    void restoreSystemProperties() {
        restore(CommandLineUsagePropertyProvider.DISABLED_KEY, originalDisabled);
        restore(CommandLineUsagePropertyProvider.URL_KEY, originalUrl);
    }

    private static void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    // --- parseNullableBoolean ---

    @Test
    void parseNullableBooleanKeepsNullAndParsesTheRest() {
        assertNull(UsagePropertyProvider.parseNullableBoolean(null));
        assertEquals(Boolean.TRUE, UsagePropertyProvider.parseNullableBoolean("true"));
        assertEquals(Boolean.TRUE, UsagePropertyProvider.parseNullableBoolean("TRUE"));
        assertEquals(Boolean.FALSE, UsagePropertyProvider.parseNullableBoolean("false"));
    }

    @Test
    void parseNullableBooleanTreatsAnythingButTrueAsFalse() {
        assertEquals(Boolean.FALSE, UsagePropertyProvider.parseNullableBoolean("yes"));
        assertEquals(Boolean.FALSE, UsagePropertyProvider.parseNullableBoolean("1"));
        assertEquals(Boolean.FALSE, UsagePropertyProvider.parseNullableBoolean(""));
    }

    // --- command line ---

    @Test
    void commandLineProviderReturnsNullWhenSystemPropertiesAreNotSet() {
        CommandLineUsagePropertyProvider testSubject = new CommandLineUsagePropertyProvider();

        assertNull(testSubject.getDisabled());
        assertNull(testSubject.getUrl());
    }

    @Test
    void commandLineProviderReadsSystemProperties() {
        System.setProperty(CommandLineUsagePropertyProvider.DISABLED_KEY, "true");
        System.setProperty(CommandLineUsagePropertyProvider.URL_KEY, "https://example.org/updates");
        CommandLineUsagePropertyProvider testSubject = new CommandLineUsagePropertyProvider();

        assertTrue(testSubject.getDisabled());
        assertEquals("https://example.org/updates", testSubject.getUrl());
    }

    @Test
    void commandLineProviderReadsPropertiesOnEveryCall() {
        CommandLineUsagePropertyProvider testSubject = new CommandLineUsagePropertyProvider();
        assertNull(testSubject.getDisabled());

        System.setProperty(CommandLineUsagePropertyProvider.DISABLED_KEY, "false");

        assertFalse(testSubject.getDisabled());
    }

    @Test
    void commandLineProviderHasTheHighestPriorityAndDocumentedKeys() {
        assertEquals(Integer.MAX_VALUE, new CommandLineUsagePropertyProvider().priority());
        assertEquals("axoniq.update-checker.disabled", CommandLineUsagePropertyProvider.DISABLED_KEY);
        assertEquals("axoniq.usage.url", CommandLineUsagePropertyProvider.URL_KEY);
    }

    // --- environment variables ---

    @Test
    void environmentProviderRejectsNullSupplier() {
        NullPointerException exception = assertThrows(
                NullPointerException.class, () -> new EnvironmentVariableUsagePropertyProvider(null)
        );

        assertEquals("The envSupplier must not be null.", exception.getMessage());
    }

    @Test
    void environmentProviderReadsFromTheGivenSupplier() {
        Map<String, String> environment = new HashMap<>();
        environment.put(EnvironmentVariableUsagePropertyProvider.DISABLED_KEY, "true");
        environment.put(EnvironmentVariableUsagePropertyProvider.URL_KEY, "https://example.org/env");
        EnvironmentVariableUsagePropertyProvider testSubject =
                new EnvironmentVariableUsagePropertyProvider(environment::get);

        assertTrue(testSubject.getDisabled());
        assertEquals("https://example.org/env", testSubject.getUrl());
    }

    @Test
    void environmentProviderReturnsNullWhenVariablesAreMissing() {
        EnvironmentVariableUsagePropertyProvider testSubject = new EnvironmentVariableUsagePropertyProvider(key -> null);

        assertNull(testSubject.getDisabled());
        assertNull(testSubject.getUrl());
    }

    @Test
    void environmentProviderHasPriorityBelowCommandLineAndKnownKeys() {
        EnvironmentVariableUsagePropertyProvider testSubject = new EnvironmentVariableUsagePropertyProvider(key -> null);

        assertEquals(Integer.MAX_VALUE / 2, testSubject.priority());
        assertTrue(testSubject.priority() < new CommandLineUsagePropertyProvider().priority());
        assertEquals("AXONIQ_UPDATE_CHECKER_DISABLED", EnvironmentVariableUsagePropertyProvider.DISABLED_KEY);
        assertEquals("AXONIQ_UPDATE_CHECKER_URL", EnvironmentVariableUsagePropertyProvider.URL_KEY);
    }

    @Test
    void environmentProviderDefaultConstructorUsesTheProcessEnvironment() {
        EnvironmentVariableUsagePropertyProvider testSubject = new EnvironmentVariableUsagePropertyProvider();

        assertDoesNotThrow(testSubject::getDisabled);
        assertDoesNotThrow(testSubject::getUrl);
    }
}
