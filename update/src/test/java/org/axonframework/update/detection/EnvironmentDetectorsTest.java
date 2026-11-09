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

package org.axonframework.update.detection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link TestEnvironmentDetector}, the {@link MachineId} and the {@link KotlinVersion}.
 * <p>
 * {@link MachineId} stores an identifier below {@code user.home}, so that property is redirected to a temporary
 * directory for every test.
 */
class EnvironmentDetectorsTest {

    @TempDir
    Path home;

    private String originalHome;
    private String originalForce;

    @BeforeEach
    void redirectSystemProperties() {
        originalHome = System.getProperty("user.home");
        originalForce = System.getProperty(TestEnvironmentDetector.AXONIQ_USAGE_FORCE_TEST_ENVIRONMENT);
        System.setProperty("user.home", home.toString());
        System.clearProperty(TestEnvironmentDetector.AXONIQ_USAGE_FORCE_TEST_ENVIRONMENT);
    }

    @AfterEach
    void restoreSystemProperties() {
        restore("user.home", originalHome);
        restore(TestEnvironmentDetector.AXONIQ_USAGE_FORCE_TEST_ENVIRONMENT, originalForce);
    }

    private static void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    private Path machineIdFile() {
        return home.resolve(".axoniq").resolve(".machine-id");
    }

    // --- TestEnvironmentDetector ---

    @Test
    void detectsTestEnvironmentWhenCalledFromATestRunner() {
        assertTrue(TestEnvironmentDetector.isTestEnvironment());
    }

    @Test
    void doesNotDetectTestEnvironmentOnAThreadWithoutTestFrameworkFrames() throws InterruptedException {
        AtomicBoolean detected = new AtomicBoolean(true);
        Thread thread = new Thread(() -> detected.set(TestEnvironmentDetector.isTestEnvironment()));

        thread.start();
        thread.join();

        assertFalse(detected.get());
    }

    @Test
    void forcePropertyDisablesTheDetectionEvenInsideATestRunner() {
        System.setProperty(TestEnvironmentDetector.AXONIQ_USAGE_FORCE_TEST_ENVIRONMENT, "true");

        assertFalse(TestEnvironmentDetector.isTestEnvironment());
    }

    @Test
    void forcePropertyWithAnyOtherValueKeepsTheDetectionActive() {
        System.setProperty(TestEnvironmentDetector.AXONIQ_USAGE_FORCE_TEST_ENVIRONMENT, "false");

        assertTrue(TestEnvironmentDetector.isTestEnvironment());
    }

    // --- MachineId ---

    @Test
    void machineIdIsGeneratedAndStoredWhenNoneExists() throws IOException {
        String id = new MachineId().get();

        assertEquals(id, UUID.fromString(id).toString());
        assertEquals(id, Files.readString(machineIdFile()));
    }

    @Test
    void machineIdIsStableAcrossInstances() {
        assertEquals(new MachineId().get(), new MachineId().get());
    }

    @Test
    void machineIdIsReadFromAnExistingFile() throws IOException {
        Files.createDirectories(machineIdFile().getParent());
        Files.writeString(machineIdFile(), "existing-machine-id");

        assertEquals("existing-machine-id", new MachineId().get());
    }

    @Test
    void machineIdIsRandomButStillUsableWhenTheFileCannotBeStored() throws IOException {
        Files.writeString(home.resolve(".axoniq"), "not a directory");

        String first = new MachineId().get();
        String second = new MachineId().get();

        assertNotNull(UUID.fromString(first));
        assertNotEquals(first, second);
    }

    @Test
    void machineIdIsRandomWhenUserHomeCannotBeDetermined() {
        System.clearProperty("user.home");

        String first = new MachineId().get();
        String second = new MachineId().get();

        assertNotNull(UUID.fromString(first));
        assertNotEquals(first, second);
        assertFalse(Files.exists(machineIdFile()));
    }

    // --- KotlinVersion ---

    @Test
    void kotlinVersionMatchesWhetherKotlinIsOnTheClassPath() {
        boolean kotlinPresent;
        try {
            Class.forName("kotlin.KotlinVersion");
            kotlinPresent = true;
        } catch (ClassNotFoundException e) {
            kotlinPresent = false;
        }

        String version = KotlinVersion.get();

        if (kotlinPresent) {
            assertTrue(version.matches("\\d+\\.\\d+(\\.\\d+)?.*"), version);
        } else {
            assertEquals("none", version);
        }
    }

    @Test
    void kotlinVersionIsCachedBetweenCalls() {
        assertSame(KotlinVersion.get(), KotlinVersion.get());
    }
}
