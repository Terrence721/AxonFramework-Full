package org.axonframework.extension.kotlin.eventsourcing

import org.axonframework.eventsourcing.configuration.EventSourcedEntityModule
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class EventSourcedEntityModuleExtTest {
    @Test
    fun `reified declarative builds phase`() {
        val phase = EventSourcedEntityModuleExt.declarative<String, String>()
        assertNotNull(phase)
    }
}
