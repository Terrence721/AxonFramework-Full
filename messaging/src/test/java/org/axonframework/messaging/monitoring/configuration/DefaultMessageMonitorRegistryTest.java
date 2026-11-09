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

package org.axonframework.messaging.monitoring.configuration;

import org.axonframework.common.configuration.Configuration;
import org.axonframework.common.infra.FilesystemStyleComponentDescriptor;
import org.axonframework.messaging.core.Message;
import org.axonframework.messaging.monitoring.MessageMonitor;
import org.axonframework.messaging.monitoring.MultiMessageMonitor;
import org.axonframework.messaging.monitoring.NoOpMessageMonitor;
import org.axonframework.messaging.monitoring.RecordingMessageMonitor;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link DefaultMessageMonitorRegistry}.
 */
class DefaultMessageMonitorRegistryTest {

    private final DefaultMessageMonitorRegistry testSubject = new DefaultMessageMonitorRegistry();
    private final Configuration config = (Configuration) Proxy.newProxyInstance(
            getClass().getClassLoader(), new Class<?>[]{Configuration.class},
            (proxy, method, args) -> {
                switch (method.getName()) {
                    case "equals":
                        return proxy == args[0];
                    case "hashCode":
                        return System.identityHashCode(proxy);
                    case "toString":
                        return "TestConfiguration";
                    default:
                        throw new UnsupportedOperationException(method.getName());
                }
            });

    private interface Lookup {

        MessageMonitor<?> find(DefaultMessageMonitorRegistry registry, Configuration config);
    }

    private static final List<Lookup> ALL_LOOKUPS = List.of(
            (registry, config) -> registry.commandMonitor(config, String.class, "name"),
            (registry, config) -> registry.eventMonitor(config, String.class, "name"),
            (registry, config) -> registry.queryMonitor(config, String.class, "name"),
            (registry, config) -> registry.subscriptionQueryUpdateMonitor(config, String.class, "name")
    );

    @Test
    void anEmptyRegistryHandsOutTheNoOpMonitorForEveryKindOfMessage() {
        for (Lookup lookup : ALL_LOOKUPS) {
            assertSame(NoOpMessageMonitor.INSTANCE, lookup.find(testSubject, config));
        }
    }

    @Test
    void registrationMethodsAreFluent() {
        assertSame(testSubject, testSubject.registerCommandMonitor((c, type, name) -> null));
        assertSame(testSubject, testSubject.registerEventMonitor((c, type, name) -> null));
        assertSame(testSubject, testSubject.registerQueryMonitor((c, type, name) -> null));
        assertSame(testSubject, testSubject.registerSubscriptionQueryUpdateMonitor((c, type, name) -> null));
        assertSame(testSubject, testSubject.registerMonitor((c, type, name) -> null));
    }

    @Test
    void aSingleFactoryHandsOutItsMonitorAsIs() {
        RecordingMessageMonitor monitor = new RecordingMessageMonitor("m");
        testSubject.registerCommandMonitor((c, type, name) -> monitor);

        assertSame(monitor, testSubject.commandMonitor(config, String.class, "name"));
    }

    @Test
    void aCommandMonitorIsOnlyUsedForCommands() {
        testSubject.registerCommandMonitor((c, type, name) -> new RecordingMessageMonitor("m"));

        assertSame(NoOpMessageMonitor.INSTANCE, testSubject.eventMonitor(config, String.class, "name"));
        assertSame(NoOpMessageMonitor.INSTANCE, testSubject.queryMonitor(config, String.class, "name"));
        assertSame(NoOpMessageMonitor.INSTANCE,
                   testSubject.subscriptionQueryUpdateMonitor(config, String.class, "name"));
    }

    @Test
    void eachKindOfMonitorIsRegisteredForItsOwnKindOnly() {
        RecordingMessageMonitor event = new RecordingMessageMonitor("event");
        RecordingMessageMonitor query = new RecordingMessageMonitor("query");
        RecordingMessageMonitor update = new RecordingMessageMonitor("update");
        testSubject.registerEventMonitor((c, type, name) -> event)
                   .registerQueryMonitor((c, type, name) -> query)
                   .registerSubscriptionQueryUpdateMonitor((c, type, name) -> update);

        assertSame(event, testSubject.eventMonitor(config, String.class, null));
        assertSame(query, testSubject.queryMonitor(config, String.class, null));
        assertSame(update, testSubject.subscriptionQueryUpdateMonitor(config, String.class, null));
        assertSame(NoOpMessageMonitor.INSTANCE, testSubject.commandMonitor(config, String.class, null));
    }

    @Test
    void aGenericFactoryIsUsedForEveryKindOfMessage() {
        RecordingMessageMonitor monitor = new RecordingMessageMonitor("m");
        testSubject.registerMonitor((c, type, name) -> monitor);

        for (Lookup lookup : ALL_LOOKUPS) {
            assertSame(monitor, lookup.find(testSubject, config));
        }
    }

    @Test
    void multipleFactoriesAreCombinedInRegistrationOrder() {
        List<String> shared = new ArrayList<>();
        testSubject.registerCommandMonitor((c, type, name) -> new RecordingMessageMonitor("first", shared))
                   .registerCommandMonitor((c, type, name) -> new RecordingMessageMonitor("second", shared));

        MessageMonitor<?> result = testSubject.commandMonitor(config, String.class, "name");

        MultiMessageMonitor<?> multi = assertInstanceOf(MultiMessageMonitor.class, result);
        assertEquals(2, multi.messageMonitors().size());
    }

    @Test
    void factoriesReturningNullOrTheNoOpMonitorAreIgnored() {
        RecordingMessageMonitor monitor = new RecordingMessageMonitor("m");
        testSubject.registerCommandMonitor((c, type, name) -> null)
                   .registerCommandMonitor((c, type, name) -> NoOpMessageMonitor.INSTANCE)
                   .registerCommandMonitor((c, type, name) -> monitor);

        assertSame(monitor, testSubject.commandMonitor(config, String.class, "name"));
    }

    @Test
    void onlyIgnorableFactoriesYieldTheNoOpMonitor() {
        testSubject.registerCommandMonitor((c, type, name) -> null)
                   .registerCommandMonitor((c, type, name) -> NoOpMessageMonitor.INSTANCE);

        assertSame(NoOpMessageMonitor.INSTANCE, testSubject.commandMonitor(config, String.class, "name"));
    }

    @Test
    void factoriesReceiveTheConfigurationComponentTypeAndName() {
        List<Object> received = new ArrayList<>();
        testSubject.registerCommandMonitor((c, type, name) -> {
            received.add(c);
            received.add(type);
            received.add(name);
            return null;
        });

        testSubject.commandMonitor(config, Integer.class, "my-component");
        testSubject.commandMonitor(config, Long.class, null);

        assertEquals(Arrays.asList(config, Integer.class, "my-component", config, Long.class, null), received);
    }

    @Test
    void aBuilderIsResolvedAgainstTheConfiguration() {
        RecordingMessageMonitor monitor = new RecordingMessageMonitor("m");
        List<Configuration> resolvedWith = new ArrayList<>();
        testSubject.registerCommandMonitor(c -> {
            resolvedWith.add(c);
            return monitor;
        });

        assertSame(monitor, testSubject.commandMonitor(config, String.class, "name"));
        assertEquals(List.of(config), resolvedWith);
    }

    @Test
    void aGenericBuilderIsBuiltOnceAndSharedAcrossAllKindsOfMessages() {
        AtomicInteger builds = new AtomicInteger();
        testSubject.registerMonitor(c -> {
            builds.incrementAndGet();
            return new RecordingMessageMonitor("m");
        });

        MessageMonitor<?> command = testSubject.commandMonitor(config, String.class, "name");
        for (Lookup lookup : ALL_LOOKUPS) {
            assertSame(command, lookup.find(testSubject, config));
        }
        assertEquals(1, builds.get());
    }

    @Test
    void describesTheRegisteredFactoriesPerKindOfMessage() {
        FilesystemStyleComponentDescriptor descriptor = new FilesystemStyleComponentDescriptor();

        testSubject.describeTo(descriptor);

        String description = descriptor.describe();
        assertTrue(description.contains("commandMonitorFactories"), description);
        assertTrue(description.contains("eventMonitorFactories"), description);
        assertTrue(description.contains("queryMonitorFactories"), description);
        assertTrue(description.contains("subscriptionQueryUpdateMonitorFactories"), description);
    }

    @SuppressWarnings("unused")
    private static Message unused() {
        return null;
    }
}
