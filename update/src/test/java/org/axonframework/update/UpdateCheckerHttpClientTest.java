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

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import org.axonframework.update.api.Artifact;
import org.axonframework.update.api.DetectedVulnerabilitySeverity;
import org.axonframework.update.api.UpdateCheckRequest;
import org.axonframework.update.api.UpdateCheckResponse;
import org.axonframework.update.configuration.UsagePropertyProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link UpdateCheckerHttpClient} against a local {@link HttpServer}.
 */
class UpdateCheckerHttpClientTest {

    private HttpServer server;
    private final AtomicReference<String> receivedQuery = new AtomicReference<>();
    private final AtomicReference<Headers> receivedHeaders = new AtomicReference<>();
    private volatile int status = 200;
    private volatile String body = "";

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/updates", exchange -> {
            receivedQuery.set(exchange.getRequestURI().getRawQuery());
            receivedHeaders.set(exchange.getRequestHeaders());
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                exchange.getResponseBody().write(bytes);
            }
            exchange.close();
        });
        server.createContext("/moved", exchange -> {
            exchange.getResponseHeaders().add("Location", url("/updates"));
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private String url(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    private static UsagePropertyProvider providerFor(String url) {
        return new UsagePropertyProvider() {
            @Override
            public Boolean getDisabled() {
                return false;
            }

            @Override
            public String getUrl() {
                return url;
            }

            @Override
            public int priority() {
                return 0;
            }
        };
    }

    private static UpdateCheckRequest request() {
        return new UpdateCheckRequest("machine-1", "instance-1", "Linux", "6", "amd64", "21", "Vendor", "none",
                                      List.of(new Artifact("org.axonframework", "axon-messaging", "5.0.0")));
    }

    @Test
    void constructorRejectsNullPropertyProvider() {
        NullPointerException exception =
                assertThrows(NullPointerException.class, () -> new UpdateCheckerHttpClient(null));

        assertEquals("The userProperties must not be null.", exception.getMessage());
    }

    @Test
    void sendsQueryStringAndIdentifyingHeaders() {
        body = "cd=60";
        UpdateCheckerHttpClient testSubject = new UpdateCheckerHttpClient(providerFor(url("/updates")));
        UpdateCheckRequest request = request();

        testSubject.sendRequest(request, true);

        assertEquals(request.toQueryString(), receivedQuery.get());
        Headers headers = receivedHeaders.get();
        assertEquals(request.toUserAgent(), headers.getFirst("User-Agent"));
        assertEquals("machine-1", headers.getFirst("X-Machine-Id"));
        assertEquals("instance-1", headers.getFirst("X-Instance-Id"));
        assertEquals("true", headers.getFirst("X-First-Run"));
        assertTrue(Long.parseLong(headers.getFirst("X-Uptime")) >= 0);
    }

    @Test
    void marksFollowUpRequestsAsNotTheFirstRun() {
        body = "cd=60";
        UpdateCheckerHttpClient testSubject = new UpdateCheckerHttpClient(providerFor(url("/updates")));

        testSubject.sendRequest(request(), false);

        assertEquals("false", receivedHeaders.get().getFirst("X-First-Run"));
    }

    @Test
    void parsesTheResponseBodyOfASuccessfulRequest() {
        body = "cd=120\nupd=org.axonframework:axon-messaging:5.1.0\n"
                + "vul=org.axonframework:axon-messaging:5.0.1:HIGH:\"https://example.org/1\"";
        UpdateCheckerHttpClient testSubject = new UpdateCheckerHttpClient(providerFor(url("/updates")));

        Optional<UpdateCheckResponse> result = testSubject.sendRequest(request(), true);

        assertTrue(result.isPresent());
        assertEquals(120, result.get().checkInterval());
        assertEquals("5.1.0", result.get().upgrades().get(0).latestVersion());
        assertEquals(DetectedVulnerabilitySeverity.HIGH, result.get().vulnerabilities().get(0).severity());
    }

    @Test
    void emptyBodyOfASuccessfulRequestYieldsTheDefaultResponse() {
        body = "";
        UpdateCheckerHttpClient testSubject = new UpdateCheckerHttpClient(providerFor(url("/updates")));

        Optional<UpdateCheckResponse> result = testSubject.sendRequest(request(), true);

        assertTrue(result.isPresent());
        assertEquals(86400, result.get().checkInterval());
    }

    @Test
    void nonOkStatusYieldsNoResponse() {
        status = 503;
        body = "cd=60";
        UpdateCheckerHttpClient testSubject = new UpdateCheckerHttpClient(providerFor(url("/updates")));

        assertFalse(testSubject.sendRequest(request(), true).isPresent());
    }

    @Test
    void followsRedirects() {
        body = "cd=77";
        UpdateCheckerHttpClient testSubject = new UpdateCheckerHttpClient(providerFor(url("/moved")));

        Optional<UpdateCheckResponse> result = testSubject.sendRequest(request(), true);

        assertTrue(result.isPresent());
        assertEquals(77, result.get().checkInterval());
        // The redirect target was reached (its handler recorded the request headers)
        assertNotNull(receivedHeaders.get());
    }

    @Test
    void unreachableServerYieldsNoResponseInsteadOfAnException() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
            closedPort = socket.getLocalPort();
        }
        UpdateCheckerHttpClient testSubject =
                new UpdateCheckerHttpClient(providerFor("http://127.0.0.1:" + closedPort + "/updates"));

        assertFalse(testSubject.sendRequest(request(), true).isPresent());
    }

    @Test
    void malformedUrlYieldsNoResponseInsteadOfAnException() {
        UpdateCheckerHttpClient testSubject = new UpdateCheckerHttpClient(providerFor("not a url"));

        assertFalse(testSubject.sendRequest(request(), true).isPresent());
    }
}
