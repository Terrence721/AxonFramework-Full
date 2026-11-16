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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClockUtilsTest {

    @Test
    void instantReturnsCurrentInstant() {
        Instant instant = ClockUtils.instant();
        assertNotNull(instant);
        assertTrue(instant.getEpochSecond() > 0);
    }

    @Test
    void getReturnsDefaultClock() {
        Clock clock = ClockUtils.get();
        assertNotNull(clock);
        assertEquals(ZoneId.of("UTC"), clock.getZone());
    }

    @Test
    void getReturnsDefaultClockAfterReset() {
        Clock clock = ClockUtils.get();
        assertNotNull(clock);
        assertEquals(ZoneId.of("UTC"), clock.getZone());
    }

    @Test
    void setAndGetClock() {
        Clock customClock = Clock.fixed(Instant.EPOCH, ZoneId.systemDefault());
        ClockUtils.set(customClock);

        assertSame(customClock, ClockUtils.get());

        ClockUtils.reset();
        assertNotNull(ClockUtils.get());
    }

    @Test
    void resetRestoresDefaultClock() {
        Clock customClock = Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneId.systemDefault());
        ClockUtils.set(customClock);

        ClockUtils.reset();
        assertNotNull(ClockUtils.get());
        assertEquals(ZoneId.of("UTC"), ClockUtils.get().getZone());
    }

    @Test
    void setWithNullThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ClockUtils.set(null));
    }

    @Test
    void setWithNullThrowsNullPointerExceptionAfterReset() {
        assertThrows(NullPointerException.class, () -> ClockUtils.set(null));
    }

    @Test
    void instantReturnsInstantFromConfiguredClock() {
        Clock customClock = Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneId.systemDefault());
        ClockUtils.set(customClock);

        Instant instant = ClockUtils.instant();
        assertEquals(Instant.parse("2020-01-01T00:00:00Z"), instant);

        ClockUtils.reset();
    }

    @Test
    void getReturnsSameInstanceAfterSet() {
        Clock clock1 = ClockUtils.get();
        Clock clock2 = ClockUtils.get();
        assertSame(clock1, clock2);
    }

    @Test
    void setReturnsTheSetClock() {
        Clock customClock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        Clock returned = ClockUtils.set(customClock);

        assertSame(customClock, returned);
    }

    @Test
    void resetReturnsDefaultClock() {
        Clock returned = ClockUtils.reset();

        assertNotNull(returned);
        assertEquals(ZoneId.of("UTC"), returned.getZone());
    }

    @Test
    void resetReturnsDefaultClockAfterSet() {
        ClockUtils.set(Clock.systemUTC());
        Clock returned = ClockUtils.reset();

        assertNotNull(returned);
        assertEquals(ZoneId.of("UTC"), returned.getZone());
    }

    @Test
    void supplierReturnsNonNullInstant() {
        Instant instant = ClockUtils.SUPPLIER.get();
        assertNotNull(instant);
        assertTrue(instant.getEpochSecond() > 0);
    }

    @Test
    void supplierUpdatesWithClockChanges() {
        Clock customClock = Clock.fixed(Instant.EPOCH, ZoneId.systemDefault());
        ClockUtils.set(customClock);

        Instant instant = ClockUtils.SUPPLIER.get();
        assertEquals(Instant.EPOCH, instant);

        ClockUtils.reset();
    }

    @Test
    void getIsThreadSafe() {
        Clock clock1 = ClockUtils.get();
        Clock clock2 = ClockUtils.get();
        assertSame(clock1, clock2);
    }

    @Test
    void setUpdatesGlobalClockImmediately() {
        Clock customClock = Clock.fixed(Instant.parse("2020-01-01T12:00:00Z"), ZoneId.systemDefault());
        ClockUtils.set(customClock);

        Instant instant = ClockUtils.instant();
        assertEquals(Instant.parse("2020-01-01T12:00:00Z"), instant);

        ClockUtils.reset();
    }

    @Test
    void setUpdatesGlobalClockImmediatelyAfterReset() {
        ClockUtils.reset();
        Clock customClock = Clock.fixed(Instant.parse("2020-01-01T12:00:00Z"), ZoneId.systemDefault());
        ClockUtils.set(customClock);

        Instant instant = ClockUtils.instant();
        assertEquals(Instant.parse("2020-01-01T12:00:00Z"), instant);

        ClockUtils.reset();
    }
}
