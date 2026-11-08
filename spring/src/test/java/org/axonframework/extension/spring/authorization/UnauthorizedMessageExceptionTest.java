package org.axonframework.extension.spring.authorization;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UnauthorizedMessageExceptionTest {
    @Test
    void preservesMessage() {
        UnauthorizedMessageException exception = new UnauthorizedMessageException("not allowed");

        assertEquals("not allowed", exception.getMessage());
    }
}
