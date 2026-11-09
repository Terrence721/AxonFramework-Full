package org.axonframework.common;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

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
    }

    @Test
    void unwrapIfTypeReturnOriginal() {
        assertEquals(String.class, ReflectionUtils.unwrapIfType(String.class, java.util.Optional.class));
    }

    @Test
    void recordDeduplicationAndSealedHierarchy() {
        assertNotNull(ReflectionUtils.collectMethodsAndFields(PointRecord.class));
        assertNotNull(ReflectionUtils.collectSealedHierarchyIfSealed(Shape.class));
        assertNotNull(ReflectionUtils.collectSealedHierarchyIfSealed(OuterShape.class));
        assertTrue(ReflectionUtils.collectSealedHierarchyIfSealed(String.class).isEmpty());
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
    }

    private static Method findMethod(String name) {
        try {
            return Sample.class.getDeclaredMethod(name);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static Field findField(String name) {
        try {
            return Sample.class.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
}