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
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link PropertyFileUsagePropertyProvider}. The provider creates a file below
 * {@code user.home} when none exists, so every test redirects {@code user.home} to a temporary directory.
 */
class PropertyFileUsagePropertyProviderTest {

    @TempDir
    Path home;

    private String originalHome;

    @BeforeEach
    void redirectUserHome() {
        originalHome = System.getProperty("user.home");
        System.setProperty("user.home", home.toString());
    }

    @AfterEach
    void restoreUserHome() {
        if (originalHome == null) {
            System.clearProperty("user.home");
        } else {
            System.setProperty("user.home", originalHome);
        }
    }

    private Path propertiesFile() {
        return home.resolve(".axoniq").resolve("update-checker.properties");
    }

    private void writeProperties(String... lines) throws IOException {
        Files.createDirectories(propertiesFile().getParent());
        Files.write(propertiesFile(), List.of(lines));
    }

    @Test
    void createsDefaultFileWhenMissingAndReportsTheDefaults() throws IOException {
        PropertyFileUsagePropertyProvider testSubject = new PropertyFileUsagePropertyProvider();

        assertFalse(testSubject.getDisabled());
        assertEquals("https://get.axoniq.io/updates/framework", testSubject.getUrl());

        assertTrue(Files.exists(propertiesFile()));
        Properties written = new Properties();
        written.load(Files.newInputStream(propertiesFile()));
        assertEquals("https://get.axoniq.io/updates/framework", written.getProperty("telemetry_url"));
        assertEquals("false", written.getProperty("disabled"));
    }

    @Test
    void readsValuesFromAnExistingFileAndDoesNotOverwriteIt() throws IOException {
        writeProperties("telemetry_url=https://example.org/custom", "disabled=true");
        PropertyFileUsagePropertyProvider testSubject = new PropertyFileUsagePropertyProvider();

        assertTrue(testSubject.getDisabled());
        assertEquals("https://example.org/custom", testSubject.getUrl());
        assertTrue(Files.readString(propertiesFile()).contains("https://example.org/custom"));
    }

    @Test
    void missingKeysInAnExistingFileGiveNullUrlAndDisabledFalse() throws IOException {
        writeProperties("# nothing configured");
        PropertyFileUsagePropertyProvider testSubject = new PropertyFileUsagePropertyProvider();

        assertNull(testSubject.getUrl());
        assertFalse(testSubject.getDisabled());
    }

    @Test
    void loadsTheFileOnlyOnce() throws IOException {
        writeProperties("telemetry_url=https://example.org/first");
        PropertyFileUsagePropertyProvider testSubject = new PropertyFileUsagePropertyProvider();
        assertEquals("https://example.org/first", testSubject.getUrl());

        writeProperties("telemetry_url=https://example.org/second");

        assertEquals("https://example.org/first", testSubject.getUrl());
    }

    @Test
    void returnsNullWhenUserHomeCannotBeDetermined() {
        System.clearProperty("user.home");
        PropertyFileUsagePropertyProvider testSubject = new PropertyFileUsagePropertyProvider();

        assertNull(testSubject.getDisabled());
        assertNull(testSubject.getUrl());
        assertFalse(Files.exists(propertiesFile()));
    }

    @Test
    void returnsNullWhenTheDefaultFileCannotBeCreated() throws IOException {
        // A regular file where the ".axoniq" directory should be makes creating the directory impossible.
        Files.writeString(home.resolve(".axoniq"), "not a directory");
        PropertyFileUsagePropertyProvider testSubject = new PropertyFileUsagePropertyProvider();

        assertNull(testSubject.getDisabled());
        assertNull(testSubject.getUrl());
    }

    @Test
    void hasNeutralPriority() {
        assertEquals(0, new PropertyFileUsagePropertyProvider().priority());
    }

    @Test
    void createWithoutAdditionalProvidersAppliesCommandLineThenEnvironmentThenFileThenDefaults() throws IOException {
        writeProperties("telemetry_url=https://example.org/file", "disabled=true");
        String originalUrl = System.getProperty(CommandLineUsagePropertyProvider.URL_KEY);
        try {
            UsagePropertyProvider testSubject = UsagePropertyProvider.create();
            assertTrue(testSubject.getDisabled());

            System.setProperty(CommandLineUsagePropertyProvider.URL_KEY, "https://example.org/cli");
            assertEquals("https://example.org/cli", testSubject.getUrl());
        } finally {
            if (originalUrl == null) {
                System.clearProperty(CommandLineUsagePropertyProvider.URL_KEY);
            } else {
                System.setProperty(CommandLineUsagePropertyProvider.URL_KEY, originalUrl);
            }
        }
    }

    @Test
    void createIncludesAdditionalProvidersInTheHierarchy() {
        UsagePropertyProvider extra = new UsagePropertyProvider() {
            @Override
            public Boolean getDisabled() {
                return null;
            }

            @Override
            public String getUrl() {
                return "https://example.org/extra";
            }

            @Override
            public int priority() {
                return Integer.MAX_VALUE;
            }
        };
        String originalUrl = System.getProperty(CommandLineUsagePropertyProvider.URL_KEY);
        System.clearProperty(CommandLineUsagePropertyProvider.URL_KEY);
        try {
            assertEquals("https://example.org/extra", UsagePropertyProvider.create(extra).getUrl());
        } finally {
            if (originalUrl != null) {
                System.setProperty(CommandLineUsagePropertyProvider.URL_KEY, originalUrl);
            }
        }
    }
}
