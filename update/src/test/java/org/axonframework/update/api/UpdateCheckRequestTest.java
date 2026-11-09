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

/**
 * Test class validating the {@link UpdateCheckRequest}.
 */
class UpdateCheckRequestTest {

    private static UpdateCheckRequest requestWith(List<Artifact> libraries) {
        return new UpdateCheckRequest("machine", "instance", "Linux", "6.1", "amd64",
                                      "21.0.2", "Eclipse Adoptium", "2.0.0", libraries);
    }

    @Test
    void queryStringWithoutLibrariesContainsOsJavaAndKotlin() {
        String query = requestWith(List.of()).toQueryString();

        assertEquals("os=Linux%3B+6.1%3B+amd64&java=21.0.2%3B+Eclipse+Adoptium&kotlin=2.0.0", query);
    }

    @Test
    void queryStringListsEveryLibraryWithShortGroupIdAndEncodedVersion() {
        String query = requestWith(List.of(
                new Artifact("org.axonframework", "axon-messaging", "5.0.0"),
                new Artifact("org.axonframework.extensions.kotlin", "axon-kotlin", "5.0.0+build 1"),
                new Artifact("com.example", "other", "1.0")
        )).toQueryString();

        assertEquals("os=Linux%3B+6.1%3B+amd64&java=21.0.2%3B+Eclipse+Adoptium&kotlin=2.0.0"
                             + "&lib-fw.axon-messaging=5.0.0"
                             + "&lib-ext.kotlin.axon-kotlin=5.0.0%2Bbuild+1"
                             + "&lib-com.example.other=1.0", query);
    }

    @Test
    void queryStringEncodesSpecialCharactersInSystemInformation() {
        UpdateCheckRequest request = new UpdateCheckRequest("m", "i", "Windows 11", "10.0", "x86_64",
                                                            "25", "A&B=C", "none", List.of());

        assertEquals("os=Windows+11%3B+10.0%3B+x86_64&java=25%3B+A%26B%3DC&kotlin=none", request.toQueryString());
    }

    @Test
    void userAgentUsesTheDetectedAxonMessagingVersion() {
        UpdateCheckRequest request = requestWith(List.of(
                new Artifact("org.axonframework", "axon-common", "9.9.9"),
                new Artifact("org.axonframework", "axon-messaging", "5.0.0")
        ));

        assertEquals("Axoniq UpdateChecker/5.0.0 (Java 21.0.2 Eclipse Adoptium; Linux; 6.1; amd64)",
                     request.toUserAgent());
    }

    @Test
    void userAgentIgnoresMessagingArtifactsFromOtherGroups() {
        UpdateCheckRequest request =
                requestWith(List.of(new Artifact("com.example", "axon-messaging", "1.2.3")));

        assertEquals("Axoniq UpdateChecker/4.12.0 (Java 21.0.2 Eclipse Adoptium; Linux; 6.1; amd64)",
                     request.toUserAgent());
    }

    @Test
    void userAgentFallsBackToTheDefaultVersionWithoutAnyLibraries() {
        assertEquals("Axoniq UpdateChecker/4.12.0 (Java 21.0.2 Eclipse Adoptium; Linux; 6.1; amd64)",
                     requestWith(List.of()).toUserAgent());
    }

    @Test
    void recordExposesItsComponents() {
        UpdateCheckRequest request = requestWith(List.of());

        assertEquals("machine", request.machineId());
        assertEquals("instance", request.instanceId());
        assertEquals("Linux", request.osName());
        assertEquals("6.1", request.osVersion());
        assertEquals("amd64", request.osArch());
        assertEquals("21.0.2", request.jvmVersion());
        assertEquals("Eclipse Adoptium", request.jvmVendor());
        assertEquals("2.0.0", request.kotlinVersion());
        assertEquals(List.of(), request.libraries());
    }
}
