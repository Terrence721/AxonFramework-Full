package org.axonframework.common.function;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ThrowingConsumerTest {

    @Test
    void acceptsInputAndMayThrow() {
        boolean[] consumed = new boolean[1];
        ThrowingConsumer<String, RuntimeException> consumer = value -> {
            consumed[0] = true;
            if (value == null) {
                throw new RuntimeException("boom");
            }
        };

        consumer.accept("value");
        assertTrue(consumed[0]);
        assertThrows(RuntimeException.class, () -> consumer.accept(null));
    }

    private static void assertTrue(boolean value) {
        org.junit.jupiter.api.Assertions.assertTrue(value);
    }
}