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

package org.axonframework.messaging.monitoring;

import org.axonframework.messaging.core.Message;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.eventhandling.GenericEventMessage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link MessageMonitor} defaults, the {@link MultiMessageMonitor}, the
 * {@link NoOpMessageMonitor} and the {@link NoOpMessageMonitorCallback}.
 */
class MessageMonitorsTest {

    private static Message message(String payload) {
        return new GenericEventMessage(new MessageType("test.event"), payload);
    }

    // --- MessageMonitor defaults ---

    @Test
    void completeReportsSuccessWhenThereIsNoError() {
        RecordingMessageMonitor monitor = new RecordingMessageMonitor("m");
        MessageMonitor.MonitorCallback callback = monitor.onMessageIngested(message("a"));

        callback.complete().accept(null, null);

        assertTrue(monitor.events().get(1).equals("m:success"), monitor.events().toString());
    }

    @Test
    void completeReportsFailureWhenThereIsAnError() {
        RecordingMessageMonitor monitor = new RecordingMessageMonitor("m");
        MessageMonitor.MonitorCallback callback = monitor.onMessageIngested(message("a"));

        callback.complete().accept(null, new IllegalStateException("boom"));

        assertEquals("m:failure:boom", monitor.events().get(1));
    }

    @Test
    void onMessagesIngestedMapsEveryMessageToItsOwnCallback() {
        RecordingMessageMonitor monitor = new RecordingMessageMonitor("m");
        Message first = message("a");
        Message second = message("b");

        Map<? super Message, MessageMonitor.MonitorCallback> callbacks =
                monitor.onMessagesIngested(List.of(first, second));

        assertEquals(2, callbacks.size());
        assertEquals(List.of("m:ingested:" + first.identifier(), "m:ingested:" + second.identifier()),
                     monitor.events());
        callbacks.get(second).reportIgnored();
        assertEquals("m:ignored", monitor.events().get(2));
    }

    @Test
    void onMessagesIngestedOfNoMessagesIsEmpty() {
        assertTrue(new RecordingMessageMonitor("m").onMessagesIngested(List.of()).isEmpty());
    }

    // --- NoOp ---

    @Test
    void noOpMonitorIsASingletonThatHandsOutTheNoOpCallback() {
        assertSame(NoOpMessageMonitor.INSTANCE, NoOpMessageMonitor.instance());
        assertSame(NoOpMessageMonitorCallback.INSTANCE, NoOpMessageMonitor.INSTANCE.onMessageIngested(message("a")));
    }

    @Test
    void noOpCallbackIgnoresEverything() {
        MessageMonitor.MonitorCallback callback = NoOpMessageMonitorCallback.INSTANCE;

        assertDoesNotThrow(callback::reportSuccess);
        assertDoesNotThrow(callback::reportIgnored);
        assertDoesNotThrow(() -> callback.reportFailure(new IllegalStateException("ignored")));
    }

    // --- MultiMessageMonitor ---

    @Test
    void multiMonitorIngestsTheMessageInEveryMonitorInOrder() {
        List<String> shared = new ArrayList<>();
        MultiMessageMonitor<Message> testSubject = new MultiMessageMonitor<>(
                new RecordingMessageMonitor("first", shared), new RecordingMessageMonitor("second", shared));
        Message message = message("a");

        testSubject.onMessageIngested(message);

        assertEquals(List.of("first:ingested:" + message.identifier(), "second:ingested:" + message.identifier()),
                     shared);
    }

    @Test
    void multiMonitorCallbackFansOutSuccessFailureAndIgnoredToEveryMonitor() {
        List<String> shared = new ArrayList<>();
        MultiMessageMonitor<Message> testSubject = new MultiMessageMonitor<>(
                List.of(new RecordingMessageMonitor("a", shared), new RecordingMessageMonitor("b", shared)));
        MessageMonitor.MonitorCallback callback = testSubject.onMessageIngested(message("x"));
        shared.clear();

        callback.reportSuccess();
        callback.reportFailure(new IllegalStateException("boom"));
        callback.reportIgnored();

        assertEquals(List.of("a:success", "b:success", "a:failure:boom", "b:failure:boom", "a:ignored", "b:ignored"),
                     shared);
    }

    @Test
    void multiMonitorWithoutMonitorsDoesNothing() {
        MultiMessageMonitor<Message> testSubject = new MultiMessageMonitor<>(List.of());

        MessageMonitor.MonitorCallback callback = testSubject.onMessageIngested(message("a"));

        assertDoesNotThrow(callback::reportSuccess);
        assertTrue(testSubject.messageMonitors().isEmpty());
    }

    @Test
    void multiMonitorExposesItsMonitorsAsAnUnmodifiableList() {
        RecordingMessageMonitor monitor = new RecordingMessageMonitor("m");
        MultiMessageMonitor<Message> testSubject = new MultiMessageMonitor<>(monitor);

        assertEquals(List.of(monitor), testSubject.messageMonitors());
        assertThrows(UnsupportedOperationException.class, () -> testSubject.messageMonitors().add(monitor));
    }

    @Test
    void multiMonitorRejectsANullList() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new MultiMessageMonitor<Message>((List<MessageMonitor<? super Message>>) null));

        assertEquals("MessageMonitor list may not be null", exception.getMessage());
    }
}
