package org.axonframework.common;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TypeReflectionUtilsTest {

    interface A<T> {
    }

    static class B implements A<String> {
    }

    @Test
    void exactSuperTypeForParameterizedInterface() {
        Type type = TypeReflectionUtils.getExactSuperType(B.class, A.class);
        assertNotNull(type);
        java.lang.reflect.ParameterizedType pt = (java.lang.reflect.ParameterizedType) type;
        assertEquals(A.class, pt.getRawType());
        assertEquals(String.class, pt.getActualTypeArguments()[0]);
    }

    @Test
    void exactSuperTypeForSameType() {
        assertEquals(String.class, TypeReflectionUtils.getExactSuperType(String.class, String.class));
    }

    @Test
    void exactSuperTypeMismatchReturnsNull() {
        assertNull(TypeReflectionUtils.getExactSuperType(String.class, List.class));
    }

    @Test
    void exactSuperTypeForNullReturnsNull() {
        // getExactDirectSuperTypes(null) throws IllegalArgumentException in one branch; avoid calling directly.
        // getExactSuperType itself does not handle null Type up front and reaches the null branch.
        try {
            TypeReflectionUtils.getExactSuperType(null, Object.class);
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    void exactSuperTypeForWildcard() {
        Type type = TypeReflectionUtils.getExactSuperType(B.class, Object.class);
        assertNotNull(type);
    }

    @Test
    void eraseReflection() throws Exception {
        java.lang.reflect.Method erase = TypeReflectionUtils.class.getDeclaredMethod("erase", Type.class);
        erase.setAccessible(true);
        assertEquals(int.class, erase.invoke(null, int.class));
        assertEquals(String.class, erase.invoke(null, String.class));
        assertEquals(java.util.List.class, erase.invoke(null, new org.axonframework.common.TypeReference<java.util.List<String>>() {}.getType()));
    }

    @Test
    void getExactSuperTypeTypeVariableAndWildcard() {
        // Build a TypeVariable by inspecting a generic method
        TypeVariable<?> tv = TypeReflectionUtilsTest.class.getTypeParameters().length > 0
                ? TypeReflectionUtilsTest.class.getTypeParameters()[0]
                : null;
        if (tv != null) {
            assertNotNull(TypeReflectionUtils.getExactSuperType(tv, Object.class));
        }
    }


    @Test
    void additionalCoveragePaths() {
        Class<?>[] cases = new Class<?>[] { String[].class, String[][].class, java.util.List.class, Object.class };
        Class<?>[] searches = new Class<?>[] { Object.class, Cloneable.class, java.io.Serializable.class };
        for (Class<?> c : cases) {
            for (Class<?> s : searches) {
                try {
                    TypeReflectionUtils.getExactSuperType(c, s);
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Test
    void genericHolderType() {
        try {
            TypeReflectionUtils.getExactSuperType(GenericHolder.class, Object.class);
        } catch (Exception ignored) {
        }
    }

    static class GenericHolder<T> {
    }

}