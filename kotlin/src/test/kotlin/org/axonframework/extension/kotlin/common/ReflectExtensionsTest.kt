package org.axonframework.extension.kotlin.common

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.reflect.KFunction

class ReflectExtensionsTest {
    @Test
    fun `top-level kotlin function is top-level`() {
        assertTrue(::topLevelFunction.isTopLevel())
    }

    @Test
    fun `primitive kotlin array is kotlin class`() {
        assertTrue(IntArray::class.java.isKotlinClass())
    }
}

fun topLevelFunction(): Int = 1
