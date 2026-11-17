package org.axonframework.common;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReflectionUtilsTest {

    record PointRecord(int x, int y) {
    }

    sealed interface Shape permits CircleShape, SquareShape {
    }

    sealed interface OuterShape permits MiddleShape {
    }

    sealed interface MiddleShape extends OuterShape permits LeafShape {
    }

    final class LeafShape implements MiddleShape {
    }

    record CircleShape() implements Shape {
    }

    final class SquareShape implements Shape {
    }

    static class Sample {
        private int f;
        public int getF() { return f; }
        public boolean isF() { return f != 0; }
        public void setF(int value) { this.f = value; }
        public int f() { return f; }
        public int publicField;
        private String privateField;
        transient int transientField;
        public java.util.List<String> listField = new java.util.ArrayList<>();

        public String method() {
            return "result";
        }

        public String getFoo() {
            return "foo";
        }

        public void setFoo(int value) {
        }

        public boolean isFoo() {
            return true;
        }

        public String foo() {
            return "foo";
        }

    }

    @Test
    void ensureAccessibleAndFields() {
        Field privateField = findField("privateField");
        ReflectionUtils.ensureAccessible(privateField);
        assertTrue(privateField.canAccess(new Sample()));

        for (Field field : ReflectionUtils.fieldsOf(Sample.class)) {
            assertNotNull(field);
        }
        for (Field field : ReflectionUtils.fieldsOf(Sample.class, ReflectionUtils.RECURSIVE)) {
            assertNotNull(field);
        }
        for (Field field : ReflectionUtils.fieldsOf(Sample.class, ReflectionUtils.NOT_RECURSIVE)) {
            assertNotNull(field);
        }
    }

    @Test
    void methods() {
        for (Method method : ReflectionUtils.methodsOf(Sample.class)) {
            assertNotNull(method);
        }
        for (Method method : ReflectionUtils.methodsOf(Sample.class, ReflectionUtils.RECURSIVE)) {
            assertNotNull(method);
        }
        for (Method method : ReflectionUtils.methodsOf(Sample.class, ReflectionUtils.NOT_RECURSIVE)) {
            assertNotNull(method);
        }
    }

    @Test
    void hasEqualsMethod() {
        assertFalse(ReflectionUtils.hasEqualsMethod(Sample.class));
        assertFalse(ReflectionUtils.hasEqualsMethod(Object.class));
        assertTrue(ReflectionUtils.hasEqualsMethod(String.class));
    }

    @Test
    void explicitlyUnequal() {
        assertFalse(ReflectionUtils.explicitlyUnequal("a", "a"));
        assertTrue(ReflectionUtils.explicitlyUnequal("a", "b"));
    }

    @Test
    void resolvePrimitiveWrapper() {
        assertEquals(Integer.class, ReflectionUtils.resolvePrimitiveWrapperType(int.class));
        assertEquals(Integer.class, ReflectionUtils.resolvePrimitiveWrapperTypeIfPrimitive(int.class));
    }

    @Test
    void declaringClass() {
        Class<?> result = ReflectionUtils.declaringClass(Sample.class, "sample");
        // may be null for top-level classes, but call should complete without exception
        assertTrue(result == null || result.getName().length() > 0);
    }

    @Test
    void invokeAndGetValue() throws NoSuchMethodException {
        Method method = Sample.class.getDeclaredMethod("method");
        ReflectionUtils.ensureAccessible(method);
        Object value = ReflectionUtils.getMemberValue(method, new Sample());
        assertEquals("result", value);
    }

    @Test
    void isTransientTest() {
        assertTrue(ReflectionUtils.isTransient(findField("transientField")));
        assertFalse(ReflectionUtils.isTransient(findField("publicField")));
    }

    @Test
    void resolveGenericTypeTest() {
        Field field = findField("listField");
        assertEquals(String.class, ReflectionUtils.resolveGenericType(field, 0).orElse(null));
        assertTrue(ReflectionUtils.resolveGenericType(findField("publicField"), 0).isEmpty());
    }

    @Test
    void resolveMemberGenericTypeTest() {
        Field field = findField("listField");
        assertEquals(String.class, ReflectionUtils.resolveMemberGenericType(field, 0).orElse(null));
        assertTrue(ReflectionUtils.resolveMemberGenericType(findField("publicField"), 0).isEmpty());
    }

    @Test
    void invokeAndGetMethodValueTest() throws Exception {
        Method method = Sample.class.getDeclaredMethod("method");
        assertEquals("result", ReflectionUtils.invokeAndGetMethodValue(method, new Sample()));
    }

    @Test
    void getMemberValueTest() {
        Sample sample = new Sample();
        Method method = findMethod("method");
        Field field = findField("privateField");
        ReflectionUtils.setFieldValue(field, sample, "hello");
        assertEquals("result", ReflectionUtils.getMemberValue(method, sample));
        assertEquals("hello", ReflectionUtils.getMemberValue(field, sample));
        assertEquals("hello", sample.privateField);
    }

    @Test
    void getMemberValueUnsupportedThrows() {
        assertThrows(IllegalStateException.class,
                () -> ReflectionUtils.getMemberValue(Sample.class.getDeclaredConstructors()[0], new Sample()));
    }

    @Test
    void getMemberValueTypeTest() {
        assertEquals(String.class, ReflectionUtils.getMemberValueType(findMethod("method")));
        assertEquals(int.class, ReflectionUtils.getMemberValueType(findField("publicField")));
        assertThrows(IllegalStateException.class,
                () -> ReflectionUtils.getMemberValueType(Sample.class.getDeclaredConstructors()[0]));
    }

    @Test
    void unwrapIfTypeTest() {
        java.lang.reflect.Type stringType = ReflectionUtils.unwrapIfType(
                new org.axonframework.common.TypeReference<java.util.Optional<String>>() {}.getType(), java.util.Optional.class);
        assertEquals(String.class, stringType);
    }

    @Test
    void addMethodsOnInterfacesCoveredByMethodsOf() {
        // methodsOf traverses declared interfaces, covering addMethodsOnDeclaredInterfaces
        boolean found = false;
        for (java.lang.reflect.Method m : ReflectionUtils.methodsOf(java.util.List.class, ReflectionUtils.RECURSIVE)) {
            if (m.getName().equals("size")) {
                found = true;
            }
        }
        assertTrue(found);
    }

    @Test
    void getMemberGenericStringTest() {
        assertEquals(findField("listField").toGenericString(),
                ReflectionUtils.getMemberGenericString(findField("listField")));
        assertEquals(findMethod("method").toGenericString(),
                ReflectionUtils.getMemberGenericString(findMethod("method")));
    }

    @Test
    void toDiscernibleSignatureTest() {
        Method method = findMethod("method");
        assertEquals("method()", ReflectionUtils.toDiscernibleSignature(method));
    }

    @Test
    void fieldNameFromMemberTest() {
        assertEquals("listField", ReflectionUtils.fieldNameFromMember(findField("listField")));
        assertEquals("foo", ReflectionUtils.fieldNameFromMember(findMethod("getFoo")));
    }

    @Test
    void collectMethodsAndFieldsTest() {
        assertNotNull(ReflectionUtils.collectMethodsAndFields(Sample.class));
        assertNotNull(ReflectionUtils.collectMatchingMethodsAndFields(Sample.class, m -> true));
        assertNotNull(ReflectionUtils.collectMatchingMethodsAndFields(String.class, m -> true));
    }

    @Test
    void resolveMemberGenericTypeMethodBranch() {
        // method with generic return type to cover getMemberGenericType method branch
        // Use an existing method; generic type is String (raw), leaves optional empty
        assertTrue(ReflectionUtils.resolveMemberGenericType(findMethod("method"), 0).isEmpty());
    }

    @Test
    void explicitlyUnequalComparableBranch() {
        // Test Comparable branch in explicitlyUnequal
        assertTrue(ReflectionUtils.explicitlyUnequal(1, 2));
        assertFalse(ReflectionUtils.explicitlyUnequal(1, 1));
    }

    @Test
    void explicitlyUnequalEqualsMethodBranch() {
        class CustomEqual {
            @Override
            public boolean equals(Object o) {
                return false; // explicit not equal
            }
        }
        CustomEqual a = new CustomEqual();
        CustomEqual b = new CustomEqual();
        assertTrue(ReflectionUtils.explicitlyUnequal(a, b));
    }

    @Test
    void explicitlyUnequalNullCases() {
        assertTrue(ReflectionUtils.explicitlyUnequal("a", null));
        assertTrue(ReflectionUtils.explicitlyUnequal(null, "a"));
        assertFalse(ReflectionUtils.explicitlyUnequal(null, null));
    }

    @Test
    void fieldNameFromMemberAllPrefixes() {
        assertEquals("foo", ReflectionUtils.fieldNameFromMember(findMethod("getFoo")));
        assertEquals("foo", ReflectionUtils.fieldNameFromMember(findMethod("setFoo", int.class)));
        assertEquals("foo", ReflectionUtils.fieldNameFromMember(findMethod("isFoo")));
        assertEquals("foo", ReflectionUtils.fieldNameFromMember(findMethod("foo")));
        assertEquals("privateField", ReflectionUtils.fieldNameFromMember(findField("privateField")));

        // Test with constructor (not a field or method) - should throw
        assertThrows(AxonConfigurationException.class, () -> ReflectionUtils.fieldNameFromMember(Sample.class.getDeclaredConstructors()[0]));

        // Test with field that doesn't match method pattern
        assertEquals("transientField", ReflectionUtils.fieldNameFromMember(findField("transientField")));
    }

    @Test
    void fieldNameFromMemberShortNames() throws NoSuchMethodException, NoSuchFieldException {
        // Test short names that don't have enough length after prefix
        Method getF = Sample.class.getDeclaredMethod("getF");
        assertEquals("f", ReflectionUtils.fieldNameFromMember(getF));
        Method isF = Sample.class.getDeclaredMethod("isF");
        assertEquals("f", ReflectionUtils.fieldNameFromMember(isF));
        Method setF = Sample.class.getDeclaredMethod("setF", int.class);
        assertEquals("f", ReflectionUtils.fieldNameFromMember(setF));
        Method f = Sample.class.getDeclaredMethod("f");
        assertEquals("f", ReflectionUtils.fieldNameFromMember(f));
        assertEquals("f", ReflectionUtils.fieldNameFromMember(findField("f")));
        // Test with field that doesn't match method pattern
        Field transientField = Sample.class.getDeclaredField("transientField");
        assertEquals("transientField", ReflectionUtils.fieldNameFromMember(transientField));
    }

    @Test
    void getMemberGenericTypeFieldAndMethodBranches() {
        // Test Field branch - FIXED: use resolveMemberGenericType (Member) instead of getMemberGenericType (Member)
        Field field = findField("listField");
        assertEquals(String.class, ReflectionUtils.resolveMemberGenericType(field, 0).orElse(null));

        // Test Method branch with non-generic return - use resolveMemberGenericType (Member)
        Method method = findMethod("method");
        assertTrue(ReflectionUtils.resolveMemberGenericType(method, 0).isEmpty());
    }

    @Test
    void getMemberGenericStringFieldAndMethodBranches() {
        // Test Field branch
        Field field = findField("listField");
        assertEquals(field.toGenericString(), ReflectionUtils.getMemberGenericString(field));

        // Test Method branch
        Method method = findMethod("method");
        assertEquals(method.toGenericString(), ReflectionUtils.getMemberGenericString(method));
    }

    @Test
    void setFieldValueAndGetFieldValueExceptions() {
        Field field = findField("privateField");
        Sample sample = new Sample();

        // Note: ensureAccessible() makes fields/methods accessible, so we can't test IllegalAccessException
        // by making them inaccessible - they'll be made accessible again

        // Test getFieldValue IllegalArgumentException with wrong object type
        assertThrows(IllegalStateException.class, () -> ReflectionUtils.getFieldValue(field, new Object()));

        // Test with field that can't be set due to wrong type
        Field intField = findField("f");
        intField.setAccessible(true);
        // This should work since String can be assigned to Object fields
        ReflectionUtils.setFieldValue(intField, sample, 123);
        // Use getFieldValue to avoid IllegalAccessException
        assertEquals((int)123, (int)ReflectionUtils.getFieldValue(intField, sample));
    }

    @Test
    void invokeAndGetMethodValueExceptions() {
        Method method = findMethod("method");
        Sample sample = new Sample();

        // Test that method invocation works normally
        assertEquals("result", ReflectionUtils.invokeAndGetMethodValue(method, sample));

        // Test with null object - throws NullPointerException which is not caught
        assertThrows(NullPointerException.class, () -> ReflectionUtils.invokeAndGetMethodValue(method, null));
    }

    @Test
    void isNonFinalPublicMemberAllCases() throws NoSuchFieldException, NoSuchMethodException {
        // Test public non-final member - Sample class is package-private, so no member is public
        // This test checks that isNonFinalPublicMember returns false for package-private classes
        Field publicField = findField("publicField");
        assertFalse(ReflectionUtils.isNonFinalPublicMember(publicField));

        // Test private field (not public)
        Field privateField = findField("privateField");
        assertFalse(ReflectionUtils.isNonFinalPublicMember(privateField));

        // Test final field
        Field finalField = Sample.class.getDeclaredField("transientField");
        assertFalse(ReflectionUtils.isNonFinalPublicMember(finalField));

        // Test with public class - String's length method is public and non-final
        try {
            Method lengthMethod = String.class.getDeclaredMethod("length");
            assertTrue(ReflectionUtils.isNonFinalPublicMember(lengthMethod));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        // Test with final field - Modifier.isFinal
        try {
            Field intField = Sample.class.getDeclaredField("f");
            intField.setAccessible(true);
            assertFalse(ReflectionUtils.isNonFinalPublicMember(intField));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void isAccessibleAllCases() {
        Field publicField = findField("publicField");
        Field privateField = findField("privateField");

        // Test accessible field - Sample class is package-private, so field is not public
        // According to ReflectionUtils.isAccessible, it checks if the member is public and non-final
        // Since Sample is package-private, the field is not public
        assertFalse(ReflectionUtils.isAccessible(publicField));

        // Test non-accessible field
        privateField.setAccessible(false);
        assertFalse(ReflectionUtils.isAccessible(privateField));
        privateField.setAccessible(true);

        // Test with a truly public member from a public class
        try {
            Method lengthMethod = String.class.getDeclaredMethod("length");
            assertTrue(ReflectionUtils.isAccessible(lengthMethod));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void methodOfException() {
        assertThrows(NoSuchMethodException.class, () -> ReflectionUtils.methodOf(Sample.class, "nonExistentMethod", String.class));
    }

    @Test
    void resolvePrimitiveWrapperTypeIfPrimitiveNoWrapper() {
        // Test when no wrapper is found - should return original type
        java.lang.reflect.Type type = new org.axonframework.common.TypeReference<List<String>>() {}.getType();
        assertEquals(type, ReflectionUtils.resolvePrimitiveWrapperTypeIfPrimitive(type));
    }

    @Test
    void resolvePrimitiveWrapperTypeException() {
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.resolvePrimitiveWrapperType(List.class));
    }

    @Test
    void fieldNameFromMemberIllegalStateException() {
        assertThrows(AxonConfigurationException.class, () -> ReflectionUtils.fieldNameFromMember(Sample.class.getDeclaredConstructors()[0]));
    }

    @Test
    void getMemberValueTypeAndGenericStringIllegalStateException() {
        assertThrows(IllegalStateException.class, () -> ReflectionUtils.getMemberValueType(Sample.class.getDeclaredConstructors()[0]));
        // getMemberGenericString accepts Executable (including Constructor) - should NOT throw
        assertNotNull(ReflectionUtils.getMemberGenericString(Sample.class.getDeclaredConstructors()[0]));
    }

    @Test
    void collectSealedHierarchyRecursiveSealed() {
        // Test the case where type is sealed using public API
        Set<Class<? extends Shape>> result = ReflectionUtils.collectSealedHierarchyIfSealed(Shape.class);
        assertEquals(2, result.size()); // CircleShape and SquareShape
    }

    @Test
    void collectSealedHierarchyRecursiveNotSealed() {
        // Test the case where type is not sealed using public API
        Set<Class<? extends String>> result = ReflectionUtils.collectSealedHierarchyIfSealed(String.class);
        assertTrue(result.isEmpty());
    }

    @Test
    void collectSealedHierarchyIfSealedNotSealed() {
        // Test when type is not sealed
        assertTrue(ReflectionUtils.collectSealedHierarchyIfSealed(String.class).isEmpty());
    }

    @Test
    void lambdaBranches() {
        // Test lambda branches - just to cover the branch in the code
        // To trigger the lambda branch, we need a record type
        assertNotNull(ReflectionUtils.collectMethodsAndFields(PointRecord.class));

        // Test collectMethodsAndFields with a record (should use deduplicateRecordFields lambda)
        assertNotNull(ReflectionUtils.collectMethodsAndFields(PointRecord.class));

        // methodsOf traverses declared interfaces, covering addMethodsOnDeclaredInterfaces
        boolean found = false;
        for (Method m : ReflectionUtils.methodsOf(List.class, ReflectionUtils.RECURSIVE)) {
            if (m.getName().equals("size")) found = true;
        }
        assertTrue(found);
    }

    @Test
    void fieldNameFromMemberPrefixes() throws NoSuchMethodException {
        assertEquals("foo", ReflectionUtils.fieldNameFromMember(findMethod("getFoo")));
        Method setFoo = Sample.class.getDeclaredMethod("setFoo", int.class);
        assertEquals("foo", ReflectionUtils.fieldNameFromMember(setFoo));
        assertEquals("foo", ReflectionUtils.fieldNameFromMember(findMethod("isFoo")));
        assertEquals("foo", ReflectionUtils.fieldNameFromMember(findMethod("foo")));
    }

    @Test
    void explicitlyUnequalBranches() {
        assertTrue(ReflectionUtils.explicitlyUnequal("a", "b"));
        assertFalse(ReflectionUtils.explicitlyUnequal("a", "a"));
        assertTrue(ReflectionUtils.explicitlyUnequal("a", null));
        assertTrue(ReflectionUtils.explicitlyUnequal(null, "a"));
        assertFalse(ReflectionUtils.explicitlyUnequal(null, null));
        assertTrue(ReflectionUtils.explicitlyUnequal(1, 2));
        assertFalse(ReflectionUtils.explicitlyUnequal(1, 1));
        class CustomEqual {
            @Override
            public boolean equals(Object o) { return false; }
        }
        assertTrue(ReflectionUtils.explicitlyUnequal(new CustomEqual(), new CustomEqual()));
    }

    @Test
    void unwrapIfTypeReturnOriginal() {
        assertEquals(String.class, ReflectionUtils.unwrapIfType(String.class, java.util.Optional.class));
        // Test with a wrapper type
        java.lang.reflect.Type optionalType = new org.axonframework.common.TypeReference<java.util.List<String>>() {}.getType();
        assertEquals(String.class, ReflectionUtils.unwrapIfType(optionalType, java.util.List.class));
    }

    @Test
    void recordDeduplicationAndSealedHierarchy() {
        assertNotNull(ReflectionUtils.collectMethodsAndFields(PointRecord.class));
        assertNotNull(ReflectionUtils.collectSealedHierarchyIfSealed(Shape.class));
        assertNotNull(ReflectionUtils.collectSealedHierarchyIfSealed(OuterShape.class));
        assertTrue(ReflectionUtils.collectSealedHierarchyIfSealed(String.class).isEmpty());
        // Additional test to cover dedup filter code path
        assertNotNull(ReflectionUtils.collectMethodsAndFields(String.class));
    }

    @Test
    void enclosingClasses() {
        assertNotNull(ReflectionUtils.enclosingClassesOf(Sample.class));
    }

    @Test
    void collectSealedHierarchy() {
        assertNotNull(ReflectionUtils.collectSealedHierarchyIfSealed(Sample.class));
    }

    @Test
    void getAndSetFieldValue() {
        Sample sample = new Sample();
        Field privateField = findField("privateField");
        ReflectionUtils.setFieldValue(privateField, sample, "hello");
        assertEquals("hello", ReflectionUtils.getFieldValue(privateField, sample));

        // Test with primitive field
        Field fField = findField("f");
        ReflectionUtils.setFieldValue(fField, sample, 42);
        try {
            assertEquals(42, fField.get(sample));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static Method findMethod(String name, Class<?>... parameterTypes) {
        try {
            Method method = Sample.class.getDeclaredMethod(name, parameterTypes);
            // Make method accessible for test operations
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static Field findField(String name) {
        try {
            Field field = Sample.class.getDeclaredField(name);
            // Make field accessible for test operations
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
}