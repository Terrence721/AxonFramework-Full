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

package org.axonframework.messaging.tracing.configuration;

import org.axonframework.messaging.tracing.configuration.MessagingTracingSettings.SpanAttributesProviders;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link MessagingTracingSettings}. Each {@code with...} method copies all eight other
 * settings by hand, so every one of them is checked to change its own setting and nothing else.
 */
class MessagingTracingSettingsTest {

    private final MessagingTracingSettings defaults = MessagingTracingSettings.enabledByDefault();

    @Test
    void enabledByDefaultEnablesEverythingExceptTheDistributedTraceAndEventSourcingHandlers() {
        assertTrue(defaults.commandBusEnabled());
        assertTrue(defaults.eventSinkEnabled());
        assertTrue(defaults.eventProcessorEnabled());
        assertTrue(defaults.eventProcessorBatchTraceEnabled());
        assertFalse(defaults.eventProcessorDistributedInSameTrace());
        assertEquals(Duration.ofMinutes(2), defaults.eventProcessorDistributedInSameTraceTimeLimit());
        assertEquals(MessagingTracingSettings.DEFAULT_DISTRIBUTED_IN_SAME_TRACE_TIME_LIMIT,
                     defaults.eventProcessorDistributedInSameTraceTimeLimit());
        assertTrue(defaults.queryBusEnabled());
        assertFalse(defaults.eventSourcingHandlersEnabled());
        assertEquals(SpanAttributesProviders.enabledByDefault(), defaults.spanAttributesProviders());
    }

    @Test
    void spanAttributeProvidersAreAllEnabledByDefault() {
        SpanAttributesProviders providers = SpanAttributesProviders.enabledByDefault();

        assertTrue(providers.messageIdEnabled());
        assertTrue(providers.messageTypeEnabled());
        assertTrue(providers.metadataEnabled());
    }

    @Test
    void withCommandBusEnabledChangesOnlyThatSetting() {
        MessagingTracingSettings result = defaults.withCommandBusEnabled(false);

        assertFalse(result.commandBusEnabled());
        assertEquals(new MessagingTracingSettings(false, true, true, true, false, Duration.ofMinutes(2),
                                                  true, false, SpanAttributesProviders.enabledByDefault()), result);
    }

    @Test
    void withEventSinkEnabledChangesOnlyThatSetting() {
        MessagingTracingSettings result = defaults.withEventSinkEnabled(false);

        assertEquals(new MessagingTracingSettings(true, false, true, true, false, Duration.ofMinutes(2),
                                                  true, false, SpanAttributesProviders.enabledByDefault()), result);
    }

    @Test
    void withEventProcessorEnabledChangesOnlyThatSetting() {
        MessagingTracingSettings result = defaults.withEventProcessorEnabled(false);

        assertEquals(new MessagingTracingSettings(true, true, false, true, false, Duration.ofMinutes(2),
                                                  true, false, SpanAttributesProviders.enabledByDefault()), result);
    }

    @Test
    void withEventProcessorBatchTraceEnabledChangesOnlyThatSetting() {
        MessagingTracingSettings result = defaults.withEventProcessorBatchTraceEnabled(false);

        assertEquals(new MessagingTracingSettings(true, true, true, false, false, Duration.ofMinutes(2),
                                                  true, false, SpanAttributesProviders.enabledByDefault()), result);
    }

    @Test
    void withEventProcessorDistributedInSameTraceChangesOnlyThatSetting() {
        MessagingTracingSettings result = defaults.withEventProcessorDistributedInSameTrace(true);

        assertEquals(new MessagingTracingSettings(true, true, true, true, true, Duration.ofMinutes(2),
                                                  true, false, SpanAttributesProviders.enabledByDefault()), result);
    }

    @Test
    void withEventProcessorDistributedInSameTraceTimeLimitChangesOnlyThatSetting() {
        MessagingTracingSettings result =
                defaults.withEventProcessorDistributedInSameTraceTimeLimit(Duration.ofSeconds(30));

        assertEquals(new MessagingTracingSettings(true, true, true, true, false, Duration.ofSeconds(30),
                                                  true, false, SpanAttributesProviders.enabledByDefault()), result);
    }

    @Test
    void withQueryBusEnabledChangesOnlyThatSetting() {
        MessagingTracingSettings result = defaults.withQueryBusEnabled(false);

        assertEquals(new MessagingTracingSettings(true, true, true, true, false, Duration.ofMinutes(2),
                                                  false, false, SpanAttributesProviders.enabledByDefault()), result);
    }

    @Test
    void withEventSourcingHandlersEnabledChangesOnlyThatSetting() {
        MessagingTracingSettings result = defaults.withEventSourcingHandlersEnabled(true);

        assertEquals(new MessagingTracingSettings(true, true, true, true, false, Duration.ofMinutes(2),
                                                  true, true, SpanAttributesProviders.enabledByDefault()), result);
    }

    @Test
    void withSpanAttributesProvidersChangesOnlyThatSetting() {
        SpanAttributesProviders providers = new SpanAttributesProviders(false, true, false);

        MessagingTracingSettings result = defaults.withSpanAttributesProviders(providers);

        assertEquals(new MessagingTracingSettings(true, true, true, true, false, Duration.ofMinutes(2),
                                                  true, false, providers), result);
    }

    @Test
    void withMethodsReturnNewInstancesAndLeaveTheOriginalUntouched() {
        MessagingTracingSettings changed = defaults.withCommandBusEnabled(false);

        assertNotEquals(defaults, changed);
        assertTrue(defaults.commandBusEnabled());
    }
}
