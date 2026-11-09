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

package org.axonframework.update.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the parsing in {@link UpdateCheckResponse#fromRequest(String)} and the
 * {@link DetectedVulnerabilitySeverity} values it maps to.
 */
class UpdateCheckResponseTest {

    private static final int DEFAULT_INTERVAL = 86400;

    @Test
    void nullBodyYieldsTheDefaultsAndNoFindings() {
        UpdateCheckResponse response = UpdateCheckResponse.fromRequest(null);

        assertEquals(DEFAULT_INTERVAL, response.checkInterval());
        assertTrue(response.upgrades().isEmpty());
        assertTrue(response.vulnerabilities().isEmpty());
    }

    @Test
    void blankBodyYieldsTheDefaultsAndNoFindings() {
        UpdateCheckResponse response = UpdateCheckResponse.fromRequest("  \n \r\n");

        assertEquals(DEFAULT_INTERVAL, response.checkInterval());
        assertTrue(response.upgrades().isEmpty());
        assertTrue(response.vulnerabilities().isEmpty());
    }

    @Test
    void checkIntervalIsParsedFromTheCdLine() {
        assertEquals(3600, UpdateCheckResponse.fromRequest("cd=3600").checkInterval());
    }

    @Test
    void invalidCheckIntervalFallsBackToTheDefault() {
        assertEquals(DEFAULT_INTERVAL, UpdateCheckResponse.fromRequest("cd=soon").checkInterval());
        assertEquals(DEFAULT_INTERVAL, UpdateCheckResponse.fromRequest("cd=").checkInterval());
    }

    @Test
    void nonPositiveCheckIntervalFallsBackToTheDefault() {
        assertEquals(DEFAULT_INTERVAL, UpdateCheckResponse.fromRequest("cd=0").checkInterval());
        assertEquals(DEFAULT_INTERVAL, UpdateCheckResponse.fromRequest("cd=-30").checkInterval());
        assertEquals(1, UpdateCheckResponse.fromRequest("cd=1").checkInterval());
    }

    @Test
    void lastCheckIntervalLineWins() {
        assertEquals(60, UpdateCheckResponse.fromRequest("cd=3600\ncd=60").checkInterval());
    }

    @Test
    void windowsLineEndingsAndSurroundingWhitespaceAreHandled() {
        UpdateCheckResponse response =
                UpdateCheckResponse.fromRequest(" cd = 120 \r\nupd = org.example:demo:2.0.0 \r\n");

        assertEquals(120, response.checkInterval());
        assertEquals(List.of(new ArtifactAvailableUpgrade("org.example", "demo", "2.0.0")), response.upgrades());
    }

    @Test
    void linesWithoutValueAndUnknownKeysAreIgnored() {
        UpdateCheckResponse response =
                UpdateCheckResponse.fromRequest("garbage\n\nunknown=1\ncd=10\nupd\nupd=org.example:demo:2.0.0");

        assertEquals(10, response.checkInterval());
        assertEquals(1, response.upgrades().size());
        assertTrue(response.vulnerabilities().isEmpty());
    }

    @Test
    void multipleUpgradesAreCollectedInOrder() {
        UpdateCheckResponse response =
                UpdateCheckResponse.fromRequest("upd=a:one:1.1.0\nupd=b:two:2.2.0");

        assertEquals(List.of(new ArtifactAvailableUpgrade("a", "one", "1.1.0"),
                             new ArtifactAvailableUpgrade("b", "two", "2.2.0")), response.upgrades());
    }

    @Test
    void upgradesWithTheWrongNumberOfPartsAreDropped() {
        UpdateCheckResponse response =
                UpdateCheckResponse.fromRequest("upd=a:one\nupd=a:one:1.0:extra\nupd=a:one:1.0");

        assertEquals(List.of(new ArtifactAvailableUpgrade("a", "one", "1.0")), response.upgrades());
    }

    @Test
    void vulnerabilityWithQuotedUrlKeepsTheColonsOfTheUrl() {
        UpdateCheckResponse response = UpdateCheckResponse.fromRequest(
                "vul=org.example:demo:1.0.1:CRITICAL:\"https://example.org/advisory/1\"");

        assertEquals(List.of(new DetectedVulnerability("org.example", "demo", DetectedVulnerabilitySeverity.CRITICAL,
                                                       "1.0.1", "https://example.org/advisory/1")),
                     response.vulnerabilities());
    }

    @Test
    void vulnerabilityWithUnquotedUrlContainingColonsIsDropped() {
        UpdateCheckResponse response = UpdateCheckResponse.fromRequest(
                "vul=org.example:demo:1.0.1:HIGH:https://example.org/advisory/1");

        assertTrue(response.vulnerabilities().isEmpty());
    }

    @Test
    void vulnerabilitiesWithTheWrongNumberOfPartsAreDropped() {
        UpdateCheckResponse response = UpdateCheckResponse.fromRequest("vul=org.example:demo:1.0.1:HIGH");

        assertTrue(response.vulnerabilities().isEmpty());
    }

    @Test
    void severityIsMatchedCaseSensitivelyAndUnknownValuesMapToUnknown() {
        UpdateCheckResponse response = UpdateCheckResponse.fromRequest(String.join("\n",
                "vul=g:a:1:LOW:u", "vul=g:a:1:MEDIUM:u", "vul=g:a:1:HIGH:u", "vul=g:a:1:CRITICAL:u",
                "vul=g:a:1:high:u", "vul=g:a:1:SEVERE:u", "vul=g:a:1::u"));

        assertEquals(List.of(
                DetectedVulnerabilitySeverity.LOW, DetectedVulnerabilitySeverity.MEDIUM,
                DetectedVulnerabilitySeverity.HIGH, DetectedVulnerabilitySeverity.CRITICAL,
                DetectedVulnerabilitySeverity.UNKNOWN, DetectedVulnerabilitySeverity.UNKNOWN,
                DetectedVulnerabilitySeverity.UNKNOWN
        ), response.vulnerabilities().stream().map(DetectedVulnerability::severity).toList());
    }

    @Test
    void valueWithEqualsSignsIsKeptIntact() {
        UpdateCheckResponse response = UpdateCheckResponse.fromRequest(
                "vul=g:a:1:LOW:\"https://example.org/?id=1&x=2\"");

        assertEquals("https://example.org/?id=1&x=2", response.vulnerabilities().get(0).description());
    }

    @Test
    void severitiesAreDeclaredFromLeastToMostSevereWithUnknownFirst() {
        assertEquals(List.of(DetectedVulnerabilitySeverity.UNKNOWN, DetectedVulnerabilitySeverity.LOW,
                             DetectedVulnerabilitySeverity.MEDIUM, DetectedVulnerabilitySeverity.HIGH,
                             DetectedVulnerabilitySeverity.CRITICAL),
                     List.of(DetectedVulnerabilitySeverity.values()));
    }

    @Test
    void recordsExposeTheirComponents() {
        DetectedVulnerability vulnerability =
                new DetectedVulnerability("g", "a", DetectedVulnerabilitySeverity.LOW, "1.0.1", "info");
        ArtifactAvailableUpgrade upgrade = new ArtifactAvailableUpgrade("g", "a", "2.0.0");
        UpdateCheckResponse response = new UpdateCheckResponse(5, List.of(upgrade), List.of(vulnerability));

        assertEquals("g", vulnerability.groupId());
        assertEquals("a", vulnerability.artifactId());
        assertEquals(DetectedVulnerabilitySeverity.LOW, vulnerability.severity());
        assertEquals("1.0.1", vulnerability.fixVersion());
        assertEquals("info", vulnerability.description());
        assertEquals("g", upgrade.groupId());
        assertEquals("a", upgrade.artifactId());
        assertEquals("2.0.0", upgrade.latestVersion());
        assertEquals(5, response.checkInterval());
        assertEquals(List.of(upgrade), response.upgrades());
        assertEquals(List.of(vulnerability), response.vulnerabilities());
    }
}
