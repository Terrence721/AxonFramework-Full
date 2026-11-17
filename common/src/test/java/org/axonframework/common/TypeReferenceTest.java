package org.axonframework.common;

import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        assertEquals(List.class, reference.getType());
    }

    @Test
    void fromTypeWithNull() {
        assertThrows(NullPointerException.class, () -> TypeReference.fromType(null));
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
    void parameterizedTypeNotEqual() {
        TypeReference<List<String>> first = new TypeReference<List<String>>() {};
        TypeReference<List<Integer>> second = new TypeReference<List<Integer>>() {};

        assertFalse(first.isAssignableFrom(second));
        assertFalse(second.isAssignableFrom(first));
        assertNotEquals(first, second);
        assertNotEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void getType() {
        TypeReference<List<String>> reference = new TypeReference<List<String>>() {};
        assertTrue(reference.getType() instanceof ParameterizedType);
    }

    @Test
    void getTypeAsClass() {
        TypeReference<List<String>> reference = new TypeReference<List<String>>() {};
        assertEquals(List.class, reference.getTypeAsClass());
    }

    @Test
    void isAssignableFromParameterizedTypes() {
        // Both are parameterized types - should compare exact type equality
        TypeReference<List<String>> listString = new TypeReference<List<String>>() {};
        TypeReference<List<Integer>> listInteger = new TypeReference<List<Integer>>() {};

        assertTrue(listString.isAssignableFrom(listString));
        assertFalse(listString.isAssignableFrom(listInteger));
        assertFalse(listInteger.isAssignableFrom(listString));
    }

    @Test
    void isAssignableFromMixedTypes() {
        // One parameterized, one raw - should return false (defensive)
        TypeReference<List<String>> listString = new TypeReference<List<String>>() {};
        TypeReference<List> listRaw = TypeReference.fromClass(List.class);

        assertFalse(listString.isAssignableFrom(listRaw));
        assertFalse(listRaw.isAssignableFrom(listString));
    }

    @Test
    void isAssignableFromRawTypes() {
        // Both raw types - compatible
        TypeReference<List> listRaw = TypeReference.fromClass(List.class);
        TypeReference<ArrayList> arrayListRaw = TypeReference.fromClass(ArrayList.class);

        assertTrue(listRaw.isAssignableFrom(arrayListRaw));
        assertFalse(arrayListRaw.isAssignableFrom(listRaw));
    }

    @Test
    void equalsAndHashCode() {
        TypeReference<String> first = TypeReference.fromClass(String.class);
        TypeReference<String> second = TypeReference.fromClass(String.class);
        TypeReference<Integer> third = TypeReference.fromClass(Integer.class);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, third);
        assertNotEquals(first.hashCode(), third.hashCode());
        assertNotEquals(first, null);
        assertNotEquals(first, new Object());
    }


    @Test
    void fromClassWithNull() {
        assertThrows(NullPointerException.class, () -> TypeReference.fromClass(null));
    }
}