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

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class validating the {@link Artifact}, in particular its {@link Artifact#shortGroupId() short group id}.
 */
class ArtifactTest {

    private static String shortGroupIdOf(String groupId) {
        return new Artifact(groupId, "artifact", "1.0.0").shortGroupId();
    }

    @Test
    void exactKnownGroupIdsAreReducedToTheirShortCode() {
        assertEquals("fw", shortGroupIdOf("org.axonframework"));
        assertEquals("ext", shortGroupIdOf("org.axonframework.extensions"));
        assertEquals("iq", shortGroupIdOf("io.axoniq"));
    }

    @Test
    void subGroupsKeepTheirRemainderBehindTheShortCode() {
        assertEquals("fw.messaging", shortGroupIdOf("org.axonframework.messaging"));
        assertEquals("ext.spring.boot", shortGroupIdOf("org.axonframework.extensions.spring.boot"));
        assertEquals("iq.console", shortGroupIdOf("io.axoniq.console"));
    }

    @Test
    void extensionsAreRecognizedBeforeTheGeneralFrameworkPrefix() {
        assertEquals("ext.kotlin", shortGroupIdOf("org.axonframework.extensions.kotlin"));
    }

    @Test
    void unknownGroupIdsAreKeptAsIs() {
        assertEquals("com.example", shortGroupIdOf("com.example"));
        assertEquals("org.springframework", shortGroupIdOf("org.springframework"));
    }

    @Test
    void groupIdsThatMerelyStartWithAKnownPrefixAreKeptAsIs() {
        assertEquals("org.axonframeworkx", shortGroupIdOf("org.axonframeworkx"));
        assertEquals("io.axoniqx.thing", shortGroupIdOf("io.axoniqx.thing"));
    }

    @Test
    void recordsExposeTheirComponentsAndHaveValueEquality() {
        Artifact artifact = new Artifact("org.axonframework", "axon-messaging", "5.0.0");

        assertEquals("org.axonframework", artifact.groupId());
        assertEquals("axon-messaging", artifact.artifactId());
        assertEquals("5.0.0", artifact.version());
        assertEquals(new Artifact("org.axonframework", "axon-messaging", "5.0.0"), artifact);
        assertEquals(new Artifact("org.axonframework", "axon-messaging", "5.0.0").hashCode(), artifact.hashCode());
    }
}
