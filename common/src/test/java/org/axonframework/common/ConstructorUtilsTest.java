package org.axonframework.common;

import org.junit.jupiter.api.Test;

import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConstructorUtilsTest {

    static class NoArg {
        public NoArg() {
        }
    }

    static class WithArg {
        final String value;

        public WithArg() {
            this.value = "default";
        }

        public WithArg(String value) {
            this.value = value;
        }
    }

    static class NoConstructor {
        private NoConstructor() {
        }
    }

    static class ArgOnlyMismatch {
        public ArgOnlyMismatch(int value) {
        }
    }

    @Test
    void zeroArgConstructorFactory() {
        Supplier<NoArg> factory = ConstructorUtils.getConstructorFunctionWithZeroArguments(NoArg.class);
        assertNotNull(factory.get());
    }

    @Test
    void zeroArgConstructorFactoryThrowsWhenMissing() {
        assertThrows(IllegalArgumentException.class,
                () -> ConstructorUtils.getConstructorFunctionWithZeroArguments(Runnable.class));
    }

    @Test
    void factoryWithOptionalArgumentUsesArgWhenProvided() {
        Function<String, WithArg> factory = ConstructorUtils.factoryForTypeWithOptionalArgument(WithArg.class, String.class);
        assertEquals("hello", factory.apply("hello").value);
    }

    @Test
    void factoryWithOptionalArgumentFallsBackToZeroArg() {
        Function<Object, WithArg> factory = ConstructorUtils.factoryForTypeWithOptionalArgument(WithArg.class, Integer.class);
        assertEquals("default", factory.apply("ignored").value);
    }

    @Test
    void factoryThrowsWhenNoSuitableConstructor() {
        assertThrows(IllegalArgumentException.class,
                () -> ConstructorUtils.factoryForTypeWithOptionalArgument(ArgOnlyMismatch.class, String.class));
    }

}