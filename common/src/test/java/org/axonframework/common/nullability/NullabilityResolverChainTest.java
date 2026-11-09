package org.axonframework.common.nullability;

import org.junit.jupiter.api.Test;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NullabilityResolverChainTest {

    static void nullableParam(@Nullable String value) {
    }

    static void nonNullableParam(String value) {
    }

    @Test
    void resolvesNullable() throws NoSuchMethodException {
        Parameter parameter = method("nullableParam").getParameters()[0];
        assertEquals(Nullability.NULLABLE, NullabilityResolverChain.resolve(parameter));
        assertEquals(Nullability.NULLABLE, NullabilityResolver.nullabilityOf(parameter));
        assertTrue(NullabilityResolver.isNullable(parameter));
    }

    @Test
    void resolvesUnknownForPlainParameter() throws NoSuchMethodException {
        Parameter parameter = method("nonNullableParam").getParameters()[0];
        assertEquals(Nullability.UNKNOWN, NullabilityResolverChain.resolve(parameter));
        assertEquals(Nullability.UNKNOWN, NullabilityResolver.nullabilityOf(parameter));
    }

    private static Method method(String name) throws NoSuchMethodException {
        return NullabilityResolverChainTest.class.getDeclaredMethod(name, String.class);
    }

    private static void assertTrue(boolean value) {
        org.junit.jupiter.api.Assertions.assertTrue(value);
    }
}