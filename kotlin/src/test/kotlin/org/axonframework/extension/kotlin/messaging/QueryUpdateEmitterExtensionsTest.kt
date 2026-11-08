package org.axonframework.extension.kotlin.messaging

import org.axonframework.messaging.core.QualifiedName
import org.axonframework.messaging.queryhandling.QueryUpdateEmitter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class QueryUpdateEmitterExtensionsTest {
    @Test
    fun `emit uses reified query and update types`() {
        var capturedUpdate: Any? = null
        val emitter = object : QueryUpdateEmitter {
            override fun describeTo(descriptor: org.axonframework.common.infra.ComponentDescriptor) = Unit
            override fun <Q : Any> emit(queryType: Class<Q>, filter: java.util.function.Predicate<in Q>, updateSupplier: java.util.function.Supplier<Any>) {
                capturedUpdate = updateSupplier.get()
                assertTrue(filter.test("query" as Q))
            }

            override fun emit(queryName: org.axonframework.messaging.core.QualifiedName, filter: java.util.function.Predicate<Any>, updateSupplier: java.util.function.Supplier<Any>) =
                error("unused")

            override fun emit(queryName: QualifiedName, filter: java.util.function.Predicate<Any>, update: Any?) = error("unused")

            override fun <Q : Any> complete(queryType: Class<Q>, filter: java.util.function.Predicate<in Q>) = error("unused")

            override fun complete(queryName: QualifiedName, filter: java.util.function.Predicate<Any>) = error("unused")

            override fun <Q : Any> completeExceptionally(queryType: Class<Q>, filter: java.util.function.Predicate<in Q>, cause: Throwable) = error("unused")

            override fun completeExceptionally(queryName: QualifiedName, filter: java.util.function.Predicate<Any>, cause: Throwable) = error("unused")
        }

        emitter.emit(42) { q: String -> q == "query" }

        assertEquals(42, capturedUpdate)
    }
}
