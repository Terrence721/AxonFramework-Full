package org.axonframework.extension.kotlin.common

import org.axonframework.common.nullability.Nullability
import org.axonframework.common.nullability.NullabilityResolver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KotlinNullableData(val nonNull: String, val possiblyNull: String?)

class KotlinReflectNullabilityResolverTest {
    @Test
    fun `nullable kotlin parameter resolves as null`() {
        val constructor = KotlinNullableData::class.java.getDeclaredConstructor(String::class.java, String::class.java)

        val resolver: NullabilityResolver = KotlinReflectNullabilityResolver()
        assertEquals(Nullability.NON_NULL, resolver.resolve(constructor.parameters[0]))
        assertEquals(Nullability.NULLABLE, resolver.resolve(constructor.parameters[1]))
    }

    @Test
    fun `java parameters stay unknown`() {
        val constructor = String::class.java.getDeclaredConstructor(CharArray::class.java)
        val resolver: NullabilityResolver = KotlinReflectNullabilityResolver()
        assertEquals(Nullability.UNKNOWN, resolver.resolve(constructor.parameters[0]))
    }
}
