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

import org.axonframework.update.api.UpdateCheckRequest;
import org.axonframework.update.api.UpdateCheckResponse;
import org.axonframework.update.configuration.UsagePropertyProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link UpdateChecker}. The HTTP client and reporter are replaced by recording fakes, and
 * {@code user.home} is redirected to a temporary directory because the checker creates a {@code MachineId}.
 * <p>
 * The checker's own scheduling (first check after one second) is never waited for: each test calls {@code run()}
 * directly and stops the checker at the end so no scheduled run can reach the fakes afterwards.
 */
class UpdateCheckerTest {

    @TempDir
    Path home;

    private String originalHome;
    private UpdateChecker testSubject;
    private final RecordingClient client = new RecordingClient();
    private final RecordingReporter reporter = new RecordingReporter();
    private boolean disabled = false;

    @BeforeEach
    void setUp() {
        originalHome = System.getProperty("user.home");
        System.setProperty("user.home", home.toString());
        testSubject = new UpdateChecker(client, reporter, provider());
    }

    @AfterEach
    void tearDown() {
        testSubject.stop();
        if (originalHome == null) {
            System.clearProperty("user.home");
        } else {
            System.setProperty("user.home", originalHome);
        }
    }

    private UsagePropertyProvider provider() {
        return new UsagePropertyProvider() {
            @Override
            public Boolean getDisabled() {
                return disabled;
            }

            @Override
            public String getUrl() {
                return "http://localhost/unused";
            }

            @Override
            public int priority() {
                return 0;
            }
        };
    }

    @Test
    void constructorRejectsNullArguments() {
        assertEquals("The client must not be null.", assertThrows(NullPointerException.class,
                () -> new UpdateChecker(null, reporter, provider())).getMessage());
        assertEquals("The reporter must not be null.", assertThrows(NullPointerException.class,
                () -> new UpdateChecker(client, null, provider())).getMessage());
        assertEquals("The usagePropertyProvider must not be null.", assertThrows(NullPointerException.class,
                () -> new UpdateChecker(client, reporter, null)).getMessage());
    }

    @Test
    void isNotStartedInitially() {
        assertFalse(testSubject.isStarted());
    }

    @Test
    void runDoesNothingBeforeTheCheckerWasStarted() {
        testSubject.run();

        assertTrue(client.requests.isEmpty());
        assertTrue(reporter.reports.isEmpty());
    }

    @Test
    void startMarksTheCheckerAsStartedAndStartingTwiceIsHarmless() {
        testSubject.start();
        testSubject.start();

        assertTrue(testSubject.isStarted());
    }

    @Test
    void stopMarksTheCheckerAsStoppedAndPreventsFurtherRuns() {
        testSubject.start();
        testSubject.stop();
        testSubject.run();

        assertFalse(testSubject.isStarted());
        assertTrue(client.requests.isEmpty());
    }

    @Test
    void stopBeforeStartIsHarmless() {
        testSubject.stop();

        assertFalse(testSubject.isStarted());
    }

    @Test
    void optedOutCheckerNeverContactsTheServer() throws InterruptedException {
        disabled = true;

        testSubject.start();
        TimeUnit.MILLISECONDS.sleep(1_500);

        assertTrue(client.requests.isEmpty());
        assertTrue(reporter.reports.isEmpty());
    }

    @Test
    void runSendsARequestDescribingTheEnvironmentAndReportsTheResponse() {
        client.responses.add(Optional.of(new UpdateCheckResponse(3600, List.of(), List.of())));
        testSubject.start();

        testSubject.run();

        assertEquals(1, client.requests.size());
        UpdateCheckRequest request = client.requests.get(0).request;
        assertEquals(System.getProperty("os.name"), request.osName());
        assertEquals(System.getProperty("os.arch"), request.osArch());
        assertEquals(System.getProperty("java.version"), request.jvmVersion());
        assertEquals(System.getProperty("java.vendor"), request.jvmVendor());
        assertFalse(request.machineId().isBlank());
        assertEquals(1, reporter.reports.size());
        assertEquals(3600, reporter.reports.get(0).checkInterval());
    }

    @Test
    void firstRequestIsFlaggedAsFirstAndLaterOnesAreNot() {
        client.responses.add(Optional.of(new UpdateCheckResponse(3600, List.of(), List.of())));
        client.responses.add(Optional.of(new UpdateCheckResponse(3600, List.of(), List.of())));
        testSubject.start();

        testSubject.run();
        testSubject.run();

        assertTrue(client.requests.get(0).firstRequest);
        assertFalse(client.requests.get(1).firstRequest);
    }

    @Test
    void everyRequestCarriesTheSameMachineIdButAFreshInstanceId() {
        client.responses.add(Optional.of(new UpdateCheckResponse(3600, List.of(), List.of())));
        client.responses.add(Optional.of(new UpdateCheckResponse(3600, List.of(), List.of())));
        testSubject.start();

        testSubject.run();
        testSubject.run();

        assertEquals(client.requests.get(0).request.machineId(), client.requests.get(1).request.machineId());
        assertNotEquals(client.requests.get(0).request.instanceId(), client.requests.get(1).request.instanceId());
    }

    @Test
    void missingResponseIsNotReportedAndTheNextRequestIsStillTheFirst() {
        client.responses.add(Optional.empty());
        client.responses.add(Optional.of(new UpdateCheckResponse(3600, List.of(), List.of())));
        testSubject.start();

        testSubject.run();
        testSubject.run();

        assertEquals(1, reporter.reports.size());
        assertTrue(client.requests.get(0).firstRequest);
        assertTrue(client.requests.get(1).firstRequest);
    }

    @Test
    void failingClientDoesNotEscapeFromRun() {
        client.failure = new IllegalStateException("network down");
        testSubject.start();

        testSubject.run();

        assertEquals(1, client.requests.size());
        assertTrue(reporter.reports.isEmpty());
    }

    @Test
    void failingReporterDoesNotEscapeFromRun() {
        client.responses.add(Optional.of(new UpdateCheckResponse(3600, List.of(), List.of())));
        reporter.failure = new IllegalStateException("cannot report");
        testSubject.start();

        testSubject.run();

        assertEquals(1, client.requests.size());
    }

    private static final class RecordedRequest {

        private final UpdateCheckRequest request;
        private final boolean firstRequest;

        private RecordedRequest(UpdateCheckRequest request, boolean firstRequest) {
            this.request = request;
            this.firstRequest = firstRequest;
        }
    }

    private static final class RecordingClient extends UpdateCheckerHttpClient {

        private final List<RecordedRequest> requests = Collections.synchronizedList(new ArrayList<>());
        private final List<Optional<UpdateCheckResponse>> responses = new ArrayList<>();
        private RuntimeException failure;

        private RecordingClient() {
            super(new UsagePropertyProvider() {
                @Override
                public Boolean getDisabled() {
                    return false;
                }

                @Override
                public String getUrl() {
                    return "http://localhost/unused";
                }

                @Override
                public int priority() {
                    return 0;
                }
            });
        }

        @Override
        public Optional<UpdateCheckResponse> sendRequest(UpdateCheckRequest updateCheckRequest,
                                                         boolean firstRequest) {
            requests.add(new RecordedRequest(updateCheckRequest, firstRequest));
            if (failure != null) {
                throw failure;
            }
            return responses.isEmpty() ? Optional.empty() : responses.remove(0);
        }
    }

    private static final class RecordingReporter implements UpdateCheckerReporter {

        private final List<UpdateCheckResponse> reports = Collections.synchronizedList(new ArrayList<>());
        private RuntimeException failure;

        @Override
        public void report(UpdateCheckRequest request, UpdateCheckResponse response) {
            reports.add(response);
            if (failure != null) {
                throw failure;
            }
        }
    }
}
