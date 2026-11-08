package org.axonframework.extension.kotlin.common

import kotlin.reflect.KProperty1
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

data class KotlinPropertyAccessTestee(val name: String)

class KotlinReflectPropertyAccessStrategyTest {
    @Test
    fun `kProperty finds kotlin property without force unwrap`() {
        val kProperty = KotlinReflectPropertyAccessStrategy.kProperty(KotlinPropertyAccessTestee::class.java, "name")

        assertNotNull(kProperty)
        assertEquals("bar", (kProperty as KProperty1<KotlinPropertyAccessTestee, String>).call(KotlinPropertyAccessTestee("bar")))
    }

    @Test
    fun `kProperty returns null for java classes`() {
        val kProperty = KotlinReflectPropertyAccessStrategy.kProperty(String::class.java, "length")
        assertNull(kProperty)
    }
}
