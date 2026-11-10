package org.axonframework.common;

import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeReferenceTest {

    @Test
    void fromClassAndGetTypeAsClass() {
        TypeReference<String> reference = TypeReference.fromClass(String.class);

        assertEquals(String.class, reference.getTypeAsClass());
        assertEquals(String.class, reference.getType());
    }

    @Test
    void fromType() {
        TypeReference<?> reference = TypeReference.fromType(List.class);

        assertEquals(List.class, reference.getTypeAsClass());
    }

    @Test
    void rawAssignability() {
        TypeReference<Object> objectReference = new TypeReference<Object>() {};
        TypeReference<String> stringReference = new TypeReference<String>() {};

        assertTrue(objectReference.isAssignableFrom(stringReference));
        assertFalse(stringReference.isAssignableFrom(objectReference));
    }

    @Test
    void parameterizedTypeEquals() {
        TypeReference<List<String>> first = new TypeReference<List<String>>() {};
        TypeReference<List<String>> second = new TypeReference<List<String>>() {};

        assertTrue(first.isAssignableFrom(second));
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void getType() {
        TypeReference<List<String>> reference = new TypeReference<List<String>>() {};
        assertTrue(reference.getType() instanceof ParameterizedType);
    }
}