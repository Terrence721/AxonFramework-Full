package org.axonframework.common.function;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ThrowingFunctionTest {

    @Test
    void appliesInputAndMayThrow() {
        ThrowingFunction<String, Integer, RuntimeException> function = value -> {
            if (value == null) {
                throw new RuntimeException("boom");
            }
            return value.length();
        };

        assertEquals(5, function.apply("value"));
        assertThrows(RuntimeException.class, () -> function.apply(null));
    }
}