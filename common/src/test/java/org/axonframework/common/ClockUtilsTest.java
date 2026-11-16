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
 * See the License for the specific language governing permissions and limitations
 * under the License.
 */

package org.axonframework.common;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClockUtilsTest {

    @Test
    void setAndResetGlobalClock() {
        Clock fixed = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneId.systemDefault());
        ClockUtils.set(fixed);
        assertEquals(fixed, ClockUtils.get());
        assertEquals(Instant.parse("2024-01-01T00:00:00Z"), ClockUtils.instant());

        ClockUtils.reset();
        assertNotNull(ClockUtils.get());
        assertEquals(ZoneOffset.UTC, ClockUtils.get().getZone());
    }

    @Test
    void getReturnsNonNullClock() {
        Clock clock = ClockUtils.get();
        assertNotNull(clock);
        assertTrue(clock.instant().getEpochSecond() > 0);
    }

    @Test
    void instantReturnsNonNull() {
        Instant instant = ClockUtils.instant();
        assertNotNull(instant);
        assertTrue(instant.getEpochSecond() > 0);
    }

    @Test
    void resetReturnsDefaultClock() {
        Clock returned = ClockUtils.reset();
        assertNotNull(returned);
        assertEquals(ZoneOffset.UTC, returned.getZone());
    }

    @Test
    void setWithNullThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ClockUtils.set(null));
    }

    @Test
    void getIsThreadSafe() {
        Clock clock1 = ClockUtils.get();
        Clock clock2 = ClockUtils.get();
        assertEquals(clock1, clock2);
    }

    @Test
    void instantUsesConfiguredClock() {
        Clock customClock = Clock.fixed(Instant.parse("2020-01-01T12:00:00Z"), ZoneId.systemDefault());
        ClockUtils.set(customClock);

        Instant instant = ClockUtils.instant();
        assertEquals(Instant.parse("2020-01-01T12:00:00Z"), instant);

        ClockUtils.reset();
    }

    @Test
    void setReturnsTheSetClock() {
        Clock customClock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        Clock returned = ClockUtils.set(customClock);

        assertEquals(customClock, returned);
    }

    @Test
    void resetReturnsDefaultClockAfterSet() {
        ClockUtils.set(Clock.systemUTC());
        Clock returned = ClockUtils.reset();
        assertNotNull(returned);
        assertEquals(ZoneOffset.UTC, returned.getZone());
    }

    @Test
    void getUpdatesCorrectly() {
        ClockUtils.reset();
        Clock clock1 = ClockUtils.get();
        Clock clock2 = ClockUtils.get();
        assertEquals(clock1, clock2);
    }
}
