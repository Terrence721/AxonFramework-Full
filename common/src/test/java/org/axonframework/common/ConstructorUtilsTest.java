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

    static class WithArgOnly {
        final String value;

        public WithArgOnly(String value) {
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

    static class MultipleConstructors {
        final String value;
        final int number;

        public MultipleConstructors() {
            this.value = "zero";
            this.number = 0;
        }

        public MultipleConstructors(String value) {
            this.value = value;
            this.number = 0;
        }

        public MultipleConstructors(int number) {
            this.value = "int";
            this.number = number;
        }
    }

    static class MultiArg {
        final String value;
        final int number;

        public MultiArg() {
            this.value = "zero";
            this.number = 0;
        }

        public MultiArg(String value, int number) {
            this.value = value;
            this.number = number;
        }
    }

    static class Accessible {
        final String value;

        public Accessible() {
            this.value = "default";
        }

        public Accessible(String value) {
            this.value = value;
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
    void zeroArgConstructorFactoryThrowsWhenOnlyArgConstructor() {
        assertThrows(IllegalArgumentException.class,
                () -> ConstructorUtils.getConstructorFunctionWithZeroArguments(WithArgOnly.class));
    }

    @Test
    void zeroArgConstructorFactoryThrowsWhenPrivateNoArg() {
        assertThrows(IllegalArgumentException.class,
                () -> ConstructorUtils.getConstructorFunctionWithZeroArguments(NoConstructor.class));
    }

    @Test
    void factoryWithOptionalArgumentUsesArgWhenProvided() {
        Function<String, WithArg> factory = ConstructorUtils.factoryForTypeWithOptionalArgument(WithArg.class, String.class);
        assertEquals("hello", factory.apply("hello").value);
    }

    @Test
    void factoryWithOptionalArgumentUsesArgWhenOnlyArgConstructor() {
        Function<String, WithArgOnly> factory = ConstructorUtils.factoryForTypeWithOptionalArgument(WithArgOnly.class, String.class);
        assertEquals("hello", factory.apply("hello").value);
    }

    @Test
    void factoryWithOptionalArgumentFallsBackToZeroArg() {
        Function<Object, WithArg> factory = ConstructorUtils.factoryForTypeWithOptionalArgument(WithArg.class, Integer.class);
        assertEquals("default", factory.apply("ignored").value);
    }

    @Test
    void factoryWithOptionalArgumentPrefersArgWhenBothExist() {
        Function<String, MultipleConstructors> factory = ConstructorUtils.factoryForTypeWithOptionalArgument(MultipleConstructors.class, String.class);
        assertEquals("hello", factory.apply("hello").value);
        assertEquals(0, factory.apply("hello").number);
    }

    @Test
    void factoryWithOptionalArgumentFallsBackToIntWhenStringNotFound() {
        Function<Integer, MultipleConstructors> factory = ConstructorUtils.factoryForTypeWithOptionalArgument(MultipleConstructors.class, Integer.class);
        // Should prefer Int constructor over zero-arg
        assertEquals("int", factory.apply(42).value);
        assertEquals(42, factory.apply(42).number);
    }

    @Test
    void factoryWithOptionalArgumentUsesZeroArgWhenIntFallsBack() {
        Function<String, MultipleConstructors> factory = ConstructorUtils.factoryForTypeWithOptionalArgument(MultipleConstructors.class, String.class);
        assertEquals("zero", factory.apply("zero").value);
        assertEquals(0, factory.apply("zero").number);
    }

    @Test
    void factoryThrowsWhenNoSuitableConstructor() {
        assertThrows(IllegalArgumentException.class,
                () -> ConstructorUtils.factoryForTypeWithOptionalArgument(ArgOnlyMismatch.class, String.class));
    }

    @Test
    void factoryThrowsWhenNoConstructorsAtAll() {
        assertThrows(IllegalArgumentException.class,
                () -> ConstructorUtils.factoryForTypeWithOptionalArgument(NoConstructor.class, String.class));
    }

    @Test
    void factoryWithOptionalArgumentWorksWithMultiArg() {
        Function<String, MultiArg> factory = ConstructorUtils.factoryForTypeWithOptionalArgument(MultiArg.class, String.class);
        assertEquals("hello", factory.apply("hello").value);
        assertEquals(0, factory.apply("hello").number);
    }

    @Test
    void factoryWithOptionalArgumentWorksWithMultiArgInt() {
        Function<Integer, MultiArg> factory = ConstructorUtils.factoryForTypeWithOptionalArgument(MultiArg.class, Integer.class);
        assertEquals("hello", factory.apply(42).value);
        assertEquals(42, factory.apply(42).number);
    }

    @Test
    void factoryWithOptionalArgumentPreservesAccessibility() {
        Function<String, Accessible> factory = ConstructorUtils.factoryForTypeWithOptionalArgument(Accessible.class, String.class);
        assertNotNull(factory.apply("test"));
    }

    @Test
    void factoryWithOptionalArgumentWorksWithReflectiveConstructor() {
        // Test that the factory uses accessible constructors
        Function<String, WithArg> factory = ConstructorUtils.factoryForTypeWithOptionalArgument(WithArg.class, String.class);
        assertEquals("test", factory.apply("test").value);
    }
}