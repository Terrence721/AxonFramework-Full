package org.axonframework.common.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Annotation;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnnotationUtilsTest {

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
    @interface Meta {
        String value() default "meta-value";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE,
            ElementType.METHOD, ElementType.PARAMETER})
    @Meta
    @interface Wrapper {
        String value() default "wrapper-value";
        int number() default 1;
    }

    @Wrapper
    static class Annotated {
        @Wrapper
        String annotated(String parameter) {
            return parameter;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Outer {
        String text() default "outer-text";
    }

    @Outer
    static class OuterClass {
        @Outer
        static class InnerClass {
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
    void predicatesWork() throws Exception {
        Member member = Annotated.class.getDeclaredMethods()[0];

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
}
