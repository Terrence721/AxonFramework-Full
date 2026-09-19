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

package org.axonframework.update.api;

import org.axonframework.common.annotation.Internal;

/**
 * Represents an artifact with its group ID, artifact ID, and version.
 *
 * @param groupId    The group ID of the artifact.
 * @param artifactId The artifact ID of the artifact.
 * @param version    The version of the artifact.
 * @since 5.0.0
 */
@Internal
public record Artifact(
        String groupId,
        String artifactId,
        String version
) {

    private static final String[][] SHORT_GROUP_ID_PREFIXES = {
            {"org.axonframework.extensions", "ext"},
            {"org.axonframework", "fw"},
            {"io.axoniq", "iq"}
    };

    /**
     * Returns a short version of the group ID, to save bytes over the wire.
     *
     * @return The short version of the group ID, or the original if it can't be shortened.
     */
    public String shortGroupId() {
        for (String[] prefixAndShortCode : SHORT_GROUP_ID_PREFIXES) {
            String prefix = prefixAndShortCode[0];
            String shortCode = prefixAndShortCode[1];
            if (groupId.startsWith(prefix)) {
                return groupId.length() == prefix.length()
                        ? shortCode
                        : shortCode + "." + groupId.substring(prefix.length() + 1);
            }
        }
        return groupId;
    }

}
