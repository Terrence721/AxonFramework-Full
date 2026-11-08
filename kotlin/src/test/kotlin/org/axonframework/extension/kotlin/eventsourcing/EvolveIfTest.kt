package org.axonframework.extension.kotlin.eventsourcing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EvolveIfTest {
    @Test
    fun `evolveIf executes evolver when condition true`() {
        assertEquals(2, 1.evolveIf(true) { it + 1 })
    }

    @Test
    fun `evolveIf returns self when condition false`() {
        assertEquals(1, 1.evolveIf(false) { it + 1 })
    }
}
