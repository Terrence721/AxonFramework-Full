/*
 * Copyright (c) 2010-2026. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations
 * under the License.
 */

package org.axonframework.common.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnnotationUtilsTest {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Meta {
        String value() default "meta-value";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Meta
    @interface Wrapper {
        String value() default "wrapper-value";
    }

    @Wrapper
    static class Annotated {
    }

    @Test
    void isAnnotationPresentReturnsTrueForMetaAnnotation() {
        assertTrue(AnnotationUtils.isAnnotationPresent(Annotated.class, Meta.class));
    }

    @Test
    void findAnnotationAttributesIncludesOverriddenValueForMetaAnnotation() {
        Optional<Map<String, Object>> attributes =
                AnnotationUtils.findAnnotationAttributes(Annotated.class, Meta.class, AnnotationUtils.ADD_ALL);

        assertTrue(attributes.isPresent());
        assertEquals("meta-value", attributes.get().get("meta"));
        assertEquals("wrapper-value", attributes.get().get("wrapper"));
    }

    @Test
    void findAnnotationAttributesForMissingAnnotationIsEmpty() {
        assertFalse(AnnotationUtils.findAnnotationAttributes(String.class, Wrapper.class).isPresent());
    }
}
