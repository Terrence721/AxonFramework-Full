package org.axonframework.extension.kotlin.messaging

import org.axonframework.common.infra.ComponentDescriptor
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.axonframework.messaging.commandhandling.gateway.CommandResult
import org.axonframework.messaging.commandhandling.gateway.FutureCommandResult
import org.axonframework.messaging.core.GenericMessage
import org.axonframework.messaging.core.MessageType
import org.axonframework.messaging.core.Metadata
import org.axonframework.messaging.core.unitofwork.ProcessingContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture

class CommandGatewayExtensionsTest {
    @Test
    fun `sendWithResult and sendAndWait delegate with reified class`() {
        val gateway = object : CommandGateway {
            override fun send(command: Any, metadata: Metadata, context: ProcessingContext?): CommandResult =
                FutureCommandResult(CompletableFuture.completedFuture(
                    GenericMessage(MessageType("example", "Result", MessageType.DEFAULT_VERSION), "result")
                ))

            override fun describeTo(descriptor: ComponentDescriptor) = Unit
        }

        assertNotNull(gateway.sendWithResult<String>("command").get())
        assertEquals("result", gateway.sendAndWait<String>("command"))
    }
}
