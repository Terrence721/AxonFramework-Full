package org.axonframework.extension.kotlin.messaging

import org.axonframework.messaging.core.Metadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AxonMetadataExtensionsTest {
    @Test
    fun `metadata and extension adds entry in kotlin style`() {
        val metadata = Metadata.emptyInstance().and("key" to "value")
        assertEquals("value", metadata["key"])
    }

    @Test
    fun `metadata contains reports subset correctly`() {
        val first = Metadata.emptyInstance().and("key" to "value")
        val second = Metadata.emptyInstance().and("key" to "other")
        assertTrue(first.contains(first))
        org.junit.jupiter.api.Assertions.assertFalse(first.contains(second))
    }
}
