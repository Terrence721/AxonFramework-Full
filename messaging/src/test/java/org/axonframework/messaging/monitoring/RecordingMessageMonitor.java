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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A {@link MessageMonitor} that records everything reported to it, for use in the monitoring tests.
 */
public class RecordingMessageMonitor implements MessageMonitor<Message> {

    private final String name;
    private final List<String> events;

    /**
     * Creates a monitor that records into its own event list.
     */
    public RecordingMessageMonitor(String name) {
        this(name, Collections.synchronizedList(new ArrayList<>()));
    }

    /**
     * Creates a monitor that records into the given, possibly shared, event list.
     */
    public RecordingMessageMonitor(String name, List<String> events) {
        this.name = name;
        this.events = events;
    }

    /**
     * The recorded events, each prefixed with the monitor's name.
     */
    public List<String> events() {
        return events;
    }

    @Override
    public MonitorCallback onMessageIngested(Message message) {
        events.add(name + ":ingested:" + message.identifier());
        return new MonitorCallback() {
            @Override
            public void reportSuccess() {
                events.add(name + ":success");
            }

            @Override
            public void reportFailure(Throwable cause) {
                events.add(name + ":failure:" + cause.getMessage());
            }

            @Override
            public void reportIgnored() {
                events.add(name + ":ignored");
            }
        };
    }
}
