package org.axonframework.extension.kotlin.messaging

import org.axonframework.common.infra.ComponentDescriptor
import org.axonframework.messaging.queryhandling.gateway.QueryGateway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture

class QueryGatewayExtensionsTest {
    @Test
    fun `query extension uses reified result type`() {
        val gateway = object : QueryGateway {
            override fun describeTo(descriptor: ComponentDescriptor) = Unit

            override fun <R : Any> query(query: Any, responseType: Class<R>, context: org.axonframework.messaging.core.unitofwork.ProcessingContext?): CompletableFuture<R> =
                CompletableFuture.completedFuture("result" as R)

            override fun <R : Any> queryMany(query: Any, responseType: Class<R>, context: org.axonframework.messaging.core.unitofwork.ProcessingContext?): CompletableFuture<MutableList<R>> =
                CompletableFuture.completedFuture(mutableListOf("result" as R))

            override fun <R : Any> subscriptionQuery(query: Any, responseType: Class<R>, context: org.axonframework.messaging.core.unitofwork.ProcessingContext?, updateBufferSize: Int): org.reactivestreams.Publisher<R> =
                error("unused") as org.reactivestreams.Publisher<R>

            override fun <R : Any> streamingQuery(query: Any, responseType: Class<R>, context: org.axonframework.messaging.core.unitofwork.ProcessingContext?): org.reactivestreams.Publisher<R> =
                error("unused") as org.reactivestreams.Publisher<R>

            override fun <R : Any> subscriptionQuery(query: Any, responseType: Class<R>, mapper: java.util.function.Function<org.axonframework.messaging.queryhandling.QueryResponseMessage, R>, context: org.axonframework.messaging.core.unitofwork.ProcessingContext?, updateBufferSize: Int): org.reactivestreams.Publisher<R> =
                error("unused") as org.reactivestreams.Publisher<R>
        }

        assertEquals("result", gateway.query<String>("query").get())
        assertEquals(java.util.List.of("result"), gateway.queryMany<String>("query").get())
    }
}
