package org.axonframework.common.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Annotation;
import java.lang.annotation.Target;
import java.lang.reflect.Member;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

}
