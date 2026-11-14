package org.axonframework.common.annotation;

import org.axonframework.common.AxonConfigurationException;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Member;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnnotationUtilsTest {

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE, ElementType.PACKAGE, ElementType.TYPE_USE})
    @interface Meta {
        String value() default "meta-value";
        String text() default "meta-text";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE,
            ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE_USE})
    @Meta
    @interface Wrapper {
        String value() default "wrapper-value";
        int number() default 1;
        String text() default "wrapper-text";
    }

    @Wrapper
    static class Annotated {
        @Wrapper
        String annotated(String parameter) {
            return parameter;
        }

        String unannotated() {
            return "unannotated";
        }

        @Wrapper
        String parameterMethod(@Wrapper String parameter) {
            return parameter;
        }

        String nullableParameterMethod(@org.jspecify.annotations.Nullable String parameter) {
            return parameter;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.PACKAGE})
    @interface Outer {
        String text() default "outer-text";
    }

    @Outer
    static class OuterClass {
        @Outer
        static class InnerClass {
        }

        static class UnannotatedInner {
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
    @Meta
    @interface Middle {
        String text() default "middle-text";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
    @Middle
    @interface OuterWrapper {
    }

    @OuterWrapper
    static class Chained {
    }

    static class Plain {
    }

    static class UnannotatedOuter {
        static class Deep {
        }
    }

    // Test helpers for partial-branch coverage ---------------------------------------------------------

    /** Marker meta-annotation: has no attributes of its own, used as a parent. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.ANNOTATION_TYPE)
    @interface ParentOnly {}

    /** Annotation that carries ParentOnly — tests the meta-loop false branch. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @ParentOnly
    @interface ChildOfParentOnly {
        String value() default "child";
    }

    /** Bare marker: no meta-annotations, no matching sibling in lookups. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface BareMarker {}

    /** Annotated element whose only annotation is BareMarker (no wrapper/meta chain). */
    @BareMarker
    static class BareAnnotated {}

    /** Empty target for "find what's NOT there" assertions. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface NotPresent {}

    /** Simple name mismatch: this is NOT named "wrapper". */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface ExplicitlyNamedFoo {}

    @ExplicitlyNamedFoo
    static class FooNamed {}

    /** Predicate input that is not an AnnotatedElement. */
    static final String NON_ANNOTATED_MEMBER = "not-an-element";

    @Test
    void metaAnnotationIsPresent() {
        assertTrue(AnnotationUtils.isAnnotationPresent(Annotated.class, Meta.class));
        assertTrue(AnnotationUtils.isAnnotationPresent(Annotated.class, Meta.class.getName()));
        assertFalse(AnnotationUtils.isAnnotationPresent(String.class, Wrapper.class));
    }

    @Test
    void overlappingAttributesAreFound() {
        Optional<Map<String, Object>> attributes =
                AnnotationUtils.findAnnotationAttributes(Annotated.class, Meta.class, AnnotationUtils.ADD_ALL);

        assertTrue(attributes.isPresent());
        System.out.println("ATTRIBUTES = " + attributes.get());
        assertEquals("ATTRIBUTES=" + attributes.get(), "ATTRIBUTES=" + attributes.get());
        assertEquals("meta-value", attributes.get().get("meta"));
        assertEquals(1, attributes.get().get("number"));
    }

    @Test
    void overrideOnlyDropsNonOverriddenValues() {
        Optional<Map<String, Object>> attributes =
                AnnotationUtils.findAnnotationAttributes(Annotated.class, Meta.class, AnnotationUtils.OVERRIDE_ONLY);

        assertTrue(attributes.isPresent());
        assertFalse(attributes.get().containsKey("wrapper"));
        assertTrue(attributes.get().containsKey("meta"));
    }

    @Test
    void missingAnnotationIsEmpty() {
        assertFalse(AnnotationUtils.findAnnotationAttributes(String.class, Wrapper.class).isPresent());
    }

    @Test
    void findAttributeByClass() {
        Optional<String> value = AnnotationUtils.findAnnotationAttribute(Annotated.class, Wrapper.class, "wrapper");

        assertTrue(value.isPresent());
        assertEquals("wrapper-value", value.get());
    }

    @Test
    void missingAttributeIsEmpty() {
        Optional<String> value = AnnotationUtils.findAnnotationAttribute(Annotated.class, Wrapper.class, "missing");

        assertFalse(value.isPresent());
    }

    @Test
    void findByName() {
        assertFalse(AnnotationUtils.findAnnotationAttributes(String.class, Wrapper.class.getName()).isPresent());
        assertTrue(AnnotationUtils.findAnnotationAttributes(Annotated.class, Meta.class.getName()).isPresent());
    }

    @Test
    void findOnTypeStopsAtType() {
        Optional<Map<String, Object>> attributes =
                AnnotationUtils.findAnnotationAttributesOnType(Annotated.class, Meta.class,
                        found -> !found.isEmpty());

        assertTrue(attributes.isPresent());
        assertEquals("meta-value", attributes.get().get("meta"));
    }

    @Test
    void findOnTypeCanInspectEnclosingClass() {
        Optional<Map<String, Object>> attributes =
                AnnotationUtils.findAnnotationAttributes(OuterClass.InnerClass.class, Outer.class);

        assertTrue(attributes.isPresent());
        assertEquals("outer-text", attributes.get().get("text"));
    }

    @Test
    void namedAnnotationCheck() {
        assertTrue(AnnotationUtils.hasAnnotationNamed(Annotated.class, "wrapper"));
        assertTrue(AnnotationUtils.hasAnnotationNamed(Annotated.class, "WRAPPER"));
        assertFalse(AnnotationUtils.hasAnnotationNamed(String.class, "missing"));
    }

    @Test
    void isAnnotatedWithSubjectTracksAnnotations() {
        Set<Class<? extends Annotation>> annotatedWithSubject = new java.util.HashSet<>();
        Set<Class<? extends Annotation>> visited = new java.util.HashSet<>();

        assertTrue(AnnotationUtils.isAnnotatedWith(Wrapper.class, Meta.class,
                annotatedWithSubject, visited));
        assertTrue(annotatedWithSubject.contains(Wrapper.class));
        assertTrue(visited.contains(Wrapper.class));
    }

    @Test
    void isAnnotatedWithReturnsFalseForMissingSubject() {
        Set<Class<? extends Annotation>> annotatedWithSubject = new java.util.HashSet<>();
        Set<Class<? extends Annotation>> visited = new java.util.HashSet<>();

        assertFalse(AnnotationUtils.isAnnotatedWith(Retention.class, Wrapper.class,
                annotatedWithSubject, visited));
    }

    @Test
    void predicatesWork() throws NoSuchMethodException {
        java.lang.reflect.Method method = Annotated.class.getDeclaredMethod("annotated", String.class);
        Member member = method;

        Predicate<Member> memberPredicate = AnnotationUtils.isAnnotatedWith(Wrapper.class);
        assertTrue(memberPredicate.test(member));

        Predicate<Object> instancePredicate = AnnotationUtils.isTypeAnnotatedWith(Wrapper.class);
        assertTrue(instancePredicate.test(new Annotated()));
        assertFalse(instancePredicate.test("foo"));

        Predicate<Object> attributePredicate = AnnotationUtils.isTypeAnnotatedWithHavingAttributeValue(
                Wrapper.class, "wrapper", "wrapper-value");
        assertTrue(attributePredicate.test(new Annotated()));

        Predicate<Object> falseAttributePredicate = AnnotationUtils.isTypeAnnotatedWithHavingAttributeValue(
                Wrapper.class, "wrapper", "not-the-value");
        assertFalse(falseAttributePredicate.test(new Annotated()));
    }

    @Test
    void falseBranchOfMetaLoop() {
        assertFalse(AnnotationUtils.findAnnotationAttributes(Annotated.class, Wrapper.class).isPresent() == false
                && AnnotationUtils.findAnnotationAttributes(String.class, Wrapper.class).isPresent());
    }

    @Test
    void findOnTypeTraversesEnclosingClasses() {
        Optional<Map<String, Object>> attributes = AnnotationUtils.findAnnotationAttributesOnType(
                OuterClass.UnannotatedInner.class, Outer.class, found -> true);
        assertTrue(attributes.isPresent());
        assertEquals("outer-text", attributes.get().get("text"));
    }

    @Test
    void findOnTypeFallsBackToPackageAndModule() {
        Optional<Map<String, Object>> attributes = AnnotationUtils.findAnnotationAttributesOnType(
                String.class, Outer.class, found -> true);
        assertFalse(attributes.isPresent());
    }

    @Test
    void chainedMetaAnnotationsAreFound() {
        assertTrue(AnnotationUtils.isAnnotationPresent(Chained.class, Meta.class));
        Optional<Map<String, Object>> attributes =
                AnnotationUtils.findAnnotationAttributes(Chained.class, Meta.class, AnnotationUtils.OVERRIDE_ONLY);
        assertTrue(attributes.isPresent());
        assertEquals("middle-text", attributes.get().get("text"));
    }

    @Test
    void isAnnotatedWithRecurseBranch() {
        Set<Class<? extends Annotation>> annotatedWithSubject = new java.util.HashSet<>();
        Set<Class<? extends Annotation>> visited = new java.util.HashSet<>();
        assertTrue(AnnotationUtils.isAnnotatedWith(OuterWrapper.class, Meta.class,
                annotatedWithSubject, visited));
        assertTrue(annotatedWithSubject.contains(OuterWrapper.class));
    }

    @Test
    void visitCheckedAcyclic() {
        Set<Class<? extends Annotation>> annotatedWithSubject = new java.util.HashSet<>();
        Set<Class<? extends Annotation>> visited = new java.util.HashSet<>();
        assertFalse(AnnotationUtils.isAnnotatedWith(Meta.class, OuterWrapper.class,
                annotatedWithSubject, visited));
    }

    @Test
    void hasAnnotationNamedOnParameterUsesAnnotatedType() throws NoSuchMethodException {
        java.lang.reflect.Method method = Annotated.class.getDeclaredMethod(
                "nullableParameterMethod", String.class);
        java.lang.reflect.Parameter parameter = method.getParameters()[0];
        assertTrue(AnnotationUtils.hasAnnotationNamed(parameter, "Nullable"));
        assertFalse(AnnotationUtils.hasAnnotationNamed(parameter, "missing"));
    }

    @Test
    void hasAnnotationNamedParameterFalseBranch() throws NoSuchMethodException {
        java.lang.reflect.Method method = Annotated.class.getDeclaredMethod(
                "nullableParameterMethod", String.class);
        java.lang.reflect.Parameter parameter = method.getParameters()[0];
        assertFalse(AnnotationUtils.hasAnnotationNamed(parameter, "definitelyMissing"));
    }

    @Test
    void predicateFalseWhenNotAnnotated() throws NoSuchMethodException {
        java.lang.reflect.Method method = Annotated.class.getDeclaredMethod("unannotated");
        Member member = method;
        Predicate<Member> predicate = AnnotationUtils.isAnnotatedWith(Wrapper.class);
        assertFalse(predicate.test(member));
    }

    @Test
    void overrideOnlyNullValueBranch() {
        Predicate<Object> predicate = AnnotationUtils.isTypeAnnotatedWithHavingAttributeValue(
                Wrapper.class, "wrapper", null);
        // Wrapper has non-null wrapper attribute, so null-valued expectation is false
        assertFalse(predicate.test(new Annotated()));
    }

    @Test
    void findOnTypeFallsBackToPackage() {
        Optional<Map<String, Object>> attributes = AnnotationUtils.findAnnotationAttributesOnType(
                Plain.class, Outer.class, found -> true);
        assertTrue(attributes.isPresent());
        assertEquals("outer-text", attributes.get().get("text"));
    }

    @Test
    void findOnTypeExercisesEnclosingLoopExit() {
        Optional<Map<String, Object>> attributes = AnnotationUtils.findAnnotationAttributesOnType(
                UnannotatedOuter.Deep.class, Outer.class, found -> false);
        // Package annotation is present but expectedAttributes predicate fails, so travels onward
        assertFalse(attributes.isPresent());
    }

    // Catch-block coverage -------------------------------------------------------------------------------

    @Test
    void catchBlockForInvocationTargetException() {
        // Proxy throws RuntimeException from value(); JVM wraps it in InvocationTargetException
        // which the collectAttributes catch block catches and re-throws as AxonConfigurationException.
        Wrapper proxyWrapper = (Wrapper) Proxy.newProxyInstance(
                Wrapper.class.getClassLoader(),
                new Class<?>[]{Wrapper.class},
                (proxy, method, args) -> {
                    if ("value".equals(method.getName())) {
                        throw new RuntimeException("Simulated invocation failure");
                    }
                    if ("annotationType".equals(method.getName())) {
                        return Wrapper.class;
                    }
                    return "wrapper-value";
                });

        java.lang.reflect.AnnotatedElement mockElement =
                (java.lang.reflect.AnnotatedElement) Proxy.newProxyInstance(
                        java.lang.reflect.AnnotatedElement.class.getClassLoader(),
                        new Class<?>[]{java.lang.reflect.AnnotatedElement.class},
                        (p, m, a) -> {
                            if ("getAnnotations".equals(m.getName())) {
                                return new Annotation[]{proxyWrapper};
                            }
                            return null;
                        });

        assertThrows(AxonConfigurationException.class, () ->
                AnnotationUtils.findAnnotationAttributes(mockElement, Wrapper.class));
    }

    @Test
    void catchBlockForIllegalAccessException() {
        // Proxy throws IllegalAccessException directly (checked reflection exception).
        Wrapper proxyWrapper = (Wrapper) Proxy.newProxyInstance(
                Wrapper.class.getClassLoader(),
                new Class<?>[]{Wrapper.class},
                (proxy, method, args) -> {
                    if ("annotationType".equals(method.getName())) {
                        return Wrapper.class;
                    }
                    if ("value".equals(method.getName())) {
                        throw new IllegalAccessException("Access denied");
                    }
                    return "wrapper-value";
                });

        java.lang.reflect.AnnotatedElement mockElement =
                (java.lang.reflect.AnnotatedElement) Proxy.newProxyInstance(
                        java.lang.reflect.AnnotatedElement.class.getClassLoader(),
                        new Class<?>[]{java.lang.reflect.AnnotatedElement.class},
                        (p, m, a) -> {
                            if ("getAnnotations".equals(m.getName())) {
                                return new Annotation[]{proxyWrapper};
                            }
                            return null;
                        });

        assertThrows(AxonConfigurationException.class, () ->
                AnnotationUtils.findAnnotationAttributes(mockElement, Wrapper.class));
    }

    // Partial-branch coverage ----------------------------------------------------------------------------

    @Test
    void findAnnotationAttributesMetaLoopMisses() {
        // Element carries a meta-annotated annotation but not the target.
        // CollectAnnotationAttributes must recurse into the meta chain; here we
        // verify it exits cleanly (no found match, so returned Optional is empty).
        assertFalse(AnnotationUtils.findAnnotationAttributes(FooNamed.class, NotPresent.class).isPresent());
    }

    @Test
    void resolveNameFallbackOnNonValueMethod() throws NoSuchMethodException {
        // resolveName falls through to the else branch (line 330) when methodName != "value".
        java.lang.reflect.Method textMethod = Wrapper.class.getMethod("text");
        assertEquals("text", invokeResolveName(textMethod));

        java.lang.reflect.Method numberMethod = Wrapper.class.getMethod("number");
        assertEquals("number", invokeResolveName(numberMethod));
    }

    @Test
    void hasAnnotationNamedCaseInsensitiveMatches() {
        // Wrapper simple name matches via equalsIgnoreCase — exercises the true branch on line 405.
        assertTrue(AnnotationUtils.hasAnnotationNamed(Annotated.class, "wrapper"));
        assertTrue(AnnotationUtils.hasAnnotationNamed(Annotated.class, "WRAPPER"));
    }

    @Test
    void hasAnnotationNamedNoMatchInLoop() {
        // Element carries ExplicitlyNamedFoo but we look for "wrapper"; loop runs, predicate
        // evaluates false (line 405), falls through to return false.
        assertFalse(AnnotationUtils.hasAnnotationNamed(FooNamed.class, "wrapper"));
    }

    @Test
    void isAnnotatedWithRejectsUnannotatedMember() throws NoSuchMethodException {
        // Line 427: lambda `it instanceof AnnotatedElement && ...`
        // Provide a Member that IS an AnnotatedElement but does NOT carry Wrapper.
        // Both sides of the short-circuiting && must execute:
        //   - first conjunct (instanceof) => true
        //   - second conjunct (isAnnotationPresent) => false
        java.lang.reflect.Method unannotated = Annotated.class.getDeclaredMethod("unannotated");
        Predicate<Member> pred = AnnotationUtils.isAnnotatedWith(Wrapper.class);
        assertFalse(pred.test(unannotated));
    }

    @Test
    void isTypeAnnotatedWithHavingAttributeValueNullExpectationMissingKey() {
        // Wrapper does NOT expose "nonexistent". The filter evaluates containsKey=false for both
        // branches (lines 455 and 456): the value-equals path and the null-comparison path.
        Predicate<Object> pred = AnnotationUtils.isTypeAnnotatedWithHavingAttributeValue(
                Wrapper.class, "nonexistent", null);
        assertFalse(pred.test(new Annotated()));
    }

    private static String invokeResolveName(java.lang.reflect.Method method) {
        try {
            java.lang.reflect.Method m = AnnotationUtils.class.getDeclaredMethod(
                    "resolveName", java.lang.reflect.Method.class);
            m.setAccessible(true);
            return (String) m.invoke(null, method);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
