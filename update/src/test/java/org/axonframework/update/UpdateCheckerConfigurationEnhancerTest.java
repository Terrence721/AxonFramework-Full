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

package org.axonframework.update;

import org.axonframework.common.configuration.DefaultComponentRegistry;
import org.axonframework.update.configuration.UsagePropertyProvider;
import org.axonframework.update.detection.TestEnvironmentDetector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link UpdateCheckerConfigurationEnhancer}.
 */
class UpdateCheckerConfigurationEnhancerTest {

    private final UpdateCheckerConfigurationEnhancer testSubject = new UpdateCheckerConfigurationEnhancer();
    private final DefaultComponentRegistry registry = new DefaultComponentRegistry().disableEnhancerScanning();
    private String originalForce;

    @BeforeEach
    void rememberForceProperty() {
        originalForce = System.getProperty(TestEnvironmentDetector.AXONIQ_USAGE_FORCE_TEST_ENVIRONMENT);
    }

    @AfterEach
    void restoreForceProperty() {
        if (originalForce == null) {
            System.clearProperty(TestEnvironmentDetector.AXONIQ_USAGE_FORCE_TEST_ENVIRONMENT);
        } else {
            System.setProperty(TestEnvironmentDetector.AXONIQ_USAGE_FORCE_TEST_ENVIRONMENT, originalForce);
        }
    }

    @Test
    void runsAsTheLastEnhancer() {
        assertEquals(Integer.MAX_VALUE, testSubject.order());
        assertEquals(UpdateCheckerConfigurationEnhancer.ENHANCER_ORDER, testSubject.order());
    }

    @Test
    void registersNothingInsideATestEnvironment() {
        testSubject.enhance(registry);

        assertFalse(registry.hasComponent(UsagePropertyProvider.class));
        assertFalse(registry.hasComponent(UpdateCheckerHttpClient.class));
        assertFalse(registry.hasComponent(UpdateCheckerReporter.class));
        assertFalse(registry.hasComponent(UpdateChecker.class));
    }

    @Test
    void registersTheUpdateCheckerAndItsCollaboratorsOutsideATestEnvironment() {
        System.setProperty(TestEnvironmentDetector.AXONIQ_USAGE_FORCE_TEST_ENVIRONMENT, "true");

        testSubject.enhance(registry);

        assertTrue(registry.hasComponent(UsagePropertyProvider.class));
        assertTrue(registry.hasComponent(UpdateCheckerHttpClient.class));
        assertTrue(registry.hasComponent(UpdateCheckerReporter.class));
        assertTrue(registry.hasComponent(UpdateChecker.class));
    }

    @Test
    void doesNotReplaceComponentsThatWereRegisteredBeforehand() {
        System.setProperty(TestEnvironmentDetector.AXONIQ_USAGE_FORCE_TEST_ENVIRONMENT, "true");
        UpdateCheckerReporter custom = (request, response) -> {
        };
        registry.registerComponent(UpdateCheckerReporter.class, config -> custom);

        testSubject.enhance(registry);

        assertTrue(registry.hasComponent(UpdateCheckerReporter.class));
    }
}
