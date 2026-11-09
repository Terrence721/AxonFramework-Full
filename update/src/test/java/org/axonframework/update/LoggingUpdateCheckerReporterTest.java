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

import org.axonframework.update.api.Artifact;
import org.axonframework.update.api.ArtifactAvailableUpgrade;
import org.axonframework.update.api.DetectedVulnerability;
import org.axonframework.update.api.DetectedVulnerabilitySeverity;
import org.axonframework.update.api.UpdateCheckRequest;
import org.axonframework.update.api.UpdateCheckResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Test class validating the {@link LoggingUpdateCheckerReporter}. The reporter's static logger is deliberately
 * replaceable for testing, so it is swapped for a mock for the duration of each test.
 */
class LoggingUpdateCheckerReporterTest {

    private final LoggingUpdateCheckerReporter testSubject = new LoggingUpdateCheckerReporter();
    private Logger logger;
    private Logger originalLogger;

    @BeforeEach
    void replaceLogger() throws Exception {
        Field field = LoggingUpdateCheckerReporter.class.getDeclaredField("logger");
        field.setAccessible(true);
        originalLogger = (Logger) field.get(null);
        logger = Mockito.mock(Logger.class);
        field.set(null, logger);
    }

    @AfterEach
    void restoreLogger() throws Exception {
        Field field = LoggingUpdateCheckerReporter.class.getDeclaredField("logger");
        field.setAccessible(true);
        field.set(null, originalLogger);
    }

    private static UpdateCheckRequest request(Artifact... libraries) {
        return new UpdateCheckRequest("machine", "instance", "Linux", "6", "amd64", "21", "Vendor", "none",
                                      List.of(libraries));
    }

    @Test
    void reportsUpToDateWhenThereAreNoUpgradesOrVulnerabilities() {
        testSubject.report(request(), new UpdateCheckResponse(60, List.of(), List.of()));

        verify(logger).info("All your Axoniq libraries are up-to-date, no upgrades or vulnerabilities found.");
        verifyNoMoreInteractions(logger);
    }

    @Test
    void reportsUpgradesWithAlignedColumnsAndTheCurrentVersion() {
        UpdateCheckRequest request = request(
                new Artifact("org.axonframework", "axon-messaging", "5.0.0"),
                new Artifact("org.axonframework", "axon-common", "5.0.10")
        );
        UpdateCheckResponse response = new UpdateCheckResponse(60, List.of(
                new ArtifactAvailableUpgrade("org.axonframework", "axon-common", "5.1.0"),
                new ArtifactAvailableUpgrade("org.axonframework", "axon-messaging", "5.1.0")
        ), List.of());

        testSubject.report(request, response);

        var order = inOrder(logger);
        order.verify(logger).info("Axoniq has found the following dependency upgrade(s):");
        // Names are padded with dots to the longest name plus two, versions with spaces to the longest version
        order.verify(logger).info("{} {} -> {}", "org.axonframework:axon-common.....", "5.0.10", "5.1.0");
        order.verify(logger).info("{} {} -> {}", "org.axonframework:axon-messaging..", "5.0.0 ", "5.1.0");
        order.verify(logger).info("No vulnerabilities have been found in your Axon libraries.");
        verify(logger, never()).error(anyString(), any(Object[].class));
    }

    @Test
    void reportsUnknownCurrentVersionForUpgradesOfUndetectedArtifacts() {
        UpdateCheckResponse response = new UpdateCheckResponse(
                60, List.of(new ArtifactAvailableUpgrade("com.example", "thing", "2.0.0")), List.of()
        );

        testSubject.report(request(), response);

        // The current version of an artifact that was not detected is reported as "unknown"
        verify(logger).info("{} {} -> {}", "com.example:thing", "unknown", "2.0.0");
    }

    @Test
    void reportsVulnerabilitiesAsErrorsWithAlignedColumns() {
        UpdateCheckResponse response = new UpdateCheckResponse(60, List.of(), List.of(
                new DetectedVulnerability("org.axonframework", "axon-messaging", DetectedVulnerabilitySeverity.CRITICAL,
                                          "5.0.1", "https://example.org/1"),
                new DetectedVulnerability("io.axoniq", "x", DetectedVulnerabilitySeverity.LOW, "5.0.10", "details")
        ));

        testSubject.report(request(), response);

        var order = inOrder(logger);
        order.verify(logger).error("Axoniq has found the following vulnerabilities in your Axon libraries:");
        order.verify(logger).error("[{}] {} [Fixed in: {}] Description: {}",
                                   "CRITICAL", "org.axonframework:axon-messaging..", "5.0.1 ",
                                   "https://example.org/1");
        order.verify(logger).error("[{}] {} [Fixed in: {}] Description: {}",
                                   "LOW     ", "io.axoniq:x" + ".".repeat(23), "5.0.10", "details");
        verify(logger, never()).info(anyString());
    }

    @Test
    void reportsBothVulnerabilitiesAndUpgradesWhenBothArePresent() {
        UpdateCheckRequest request = request(new Artifact("org.axonframework", "axon-messaging", "5.0.0"));
        UpdateCheckResponse response = new UpdateCheckResponse(
                60,
                List.of(new ArtifactAvailableUpgrade("org.axonframework", "axon-messaging", "5.1.0")),
                List.of(new DetectedVulnerability("org.axonframework", "axon-messaging",
                                                  DetectedVulnerabilitySeverity.HIGH, "5.0.1", "info"))
        );

        testSubject.report(request, response);

        var order = inOrder(logger);
        order.verify(logger).error("Axoniq has found the following vulnerabilities in your Axon libraries:");
        order.verify(logger).error(eq("[{}] {} [Fixed in: {}] Description: {}"), any(), any(), any(), any());
        order.verify(logger).info("Additionally, Axoniq has found an upgrade(s) for your Axon libraries:");
        order.verify(logger).info("{} {} -> {}", "org.axonframework:axon-messaging..", "5.0.0", "5.1.0");
        verify(logger, never()).info("No vulnerabilities have been found in your Axon libraries.");
    }

    @Test
    void vulnerabilitiesWithoutUpgradesDoNotMentionUpgrades() {
        UpdateCheckResponse response = new UpdateCheckResponse(60, List.of(), List.of(
                new DetectedVulnerability("g", "a", DetectedVulnerabilitySeverity.UNKNOWN, "1", "d")));

        testSubject.report(request(), response);

        verify(logger, never()).info(anyString());
    }

    @Test
    void currentVersionLookupMatchesOnGroupAndArtifact() {
        UpdateCheckRequest request = request(
                new Artifact("org.axonframework", "axon-common", "5.0.0"),
                new Artifact("com.example", "axon-common", "9.9.9")
        );

        assertEquals("5.0.0", LoggingUpdateCheckerReporter.getCurrentVersionForArtifact(
                request, "org.axonframework", "axon-common"));
        assertEquals("9.9.9", LoggingUpdateCheckerReporter.getCurrentVersionForArtifact(
                request, "com.example", "axon-common"));
        assertEquals("unknown", LoggingUpdateCheckerReporter.getCurrentVersionForArtifact(
                request, "org.axonframework", "missing"));
    }
}
