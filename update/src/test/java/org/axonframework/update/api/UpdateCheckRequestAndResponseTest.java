package org.axonframework.update.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateCheckRequestAndResponseTest {
    @Test
    void requestCreatesQueryStringAndUserAgent() {
        UpdateCheckRequest request = new UpdateCheckRequest(
                "machine", "instance", "Linux", "6", "amd64",
                "21", "Eclipse", "none", List.of(new Artifact("org.axonframework", "axon-messaging", "1.0.0"))
        );

        String query = request.toQueryString();
        assertTrue(query.contains("os=Linux%3B"));
        assertEquals("ext.foo", new Artifact("org.axonframework.extensions.foo", "something", "1.0.0").shortGroupId());
        assertEquals("fw", new Artifact("org.axonframework", "something", "1.0.0").shortGroupId());
        assertEquals("iq", new Artifact("io.axoniq", "something", "1.0.0").shortGroupId());
        assertEquals("com.example", new Artifact("com.example", "something", "1.0.0").shortGroupId());

        org.junit.jupiter.api.Assertions.assertTrue(request.toUserAgent().contains("Java 21"));
    }

    @Test
    void responseParsesBodyLines() {
        String body = "cd=3600\nvul=org.example:demo:1.0.1:HIGH:https\nupd=org.example:demo:2.0.0";

        UpdateCheckResponse response = UpdateCheckResponse.fromRequest(body);

        assertEquals(3600, response.checkInterval());
        assertEquals(1, response.vulnerabilities().size());
        assertEquals(DetectedVulnerabilitySeverity.HIGH, response.vulnerabilities().get(0).severity());
        assertEquals("1.0.1", response.vulnerabilities().get(0).fixVersion());
        assertEquals("https", response.vulnerabilities().get(0).description());
        assertEquals(1, response.upgrades().size());
        assertEquals("2.0.0", response.upgrades().get(0).latestVersion());
    }

    @Test
    void blankResponseCreatesDefaults() {
        UpdateCheckResponse response = UpdateCheckResponse.fromRequest("");

        assertEquals(86400, response.checkInterval());
        assertTrue(response.upgrades().isEmpty());
        assertTrue(response.vulnerabilities().isEmpty());
    }
}
