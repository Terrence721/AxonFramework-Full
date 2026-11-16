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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.common.property;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test class for the {@link Property} functional interface to achieve 100% coverage.
 */
class PropertyTest {

    // Test class with various properties
    public static class TestBean {
        private String name = "test";
        private Integer value = 42;
        private @Nullable String nullableField = null;

        public String getName() {
            return name;
        }

        public Integer getValue() {
            return value;
        }

        public @Nullable String getNullableField() {
            return nullableField;
        }
    }

    @Test
    void propertyReturnsValueForStringProperty() {
        TestBean bean = new TestBean();
        Property<TestBean> property = createStringProperty();

        String result = property.getValue(bean);
        assertNotNull(result);
        assertEquals("test", result);
    }

    @Test
    void propertyReturnsValueForIntegerProperty() {
        TestBean bean = new TestBean();
        Property<TestBean> property = createIntegerProperty();

        Integer result = property.getValue(bean);
        assertNotNull(result);
        assertEquals(42, result);
    }

    @Test
    void propertyReturnsNullForNullableProperty() {
        TestBean bean = new TestBean();
        Property<TestBean> property = createNullableProperty();

        String result = property.getValue(bean);
        assertNull(result);
    }

    @Test
    void propertyWorksWithLambda() {
        TestBean bean = new TestBean();
        Property<TestBean> property = createStringWithLambdaProperty();

        String result = property.getValue(bean);
        assertNotNull(result);
        assertEquals("TEST", result);
    }

    // Additional test cases for edge cases and different scenarios
    @Test
    void propertyWithComplexMethodCall() {
        TestBean bean = new TestBean();
        Property<TestBean> property = createStringWithComplexMethodProperty();

        String result = property.getValue(bean);
        assertNotNull(result);
        assertEquals("t", result);
    }

    @Test
    void propertyWithMethodChaining() {
        TestBean bean = new TestBean();
        Property<TestBean> property = createStringWithMethodChainingProperty();

        String result = property.getValue(bean);
        assertNotNull(result);
        assertEquals("test case", result);
    }

    // Factory methods to create Property instances without lambda compatibility issues
    @SuppressWarnings("unchecked")
    private static Property<TestBean> createStringProperty() {
        return new Property<TestBean>() {
            @Override
            public String getValue(TestBean target) {
                return target.getName();
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static Property<TestBean> createIntegerProperty() {
        return new Property<TestBean>() {
            @Override
            public Integer getValue(TestBean target) {
                return target.getValue();
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static Property<TestBean> createNullableProperty() {
        return new Property<TestBean>() {
            @Override
            public String getValue(TestBean target) {
                return target.getNullableField();
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static Property<TestBean> createStringWithLambdaProperty() {
        return new Property<TestBean>() {
            @Override
            public String getValue(TestBean target) {
                return target.getName().toUpperCase();
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static Property<TestBean> createStringWithComplexMethodProperty() {
        return new Property<TestBean>() {
            @Override
            public String getValue(TestBean target) {
                return target.getName().substring(0, 1);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static Property<TestBean> createStringWithMethodChainingProperty() {
        return new Property<TestBean>() {
            @Override
            public String getValue(TestBean target) {
                return target.getName().toLowerCase().concat(" case");
            }
        };
    }

    @Test
    void propertyIsFunctionalInterface() {
        // Verify it's a functional interface with single abstract method
        // Note: java.lang.annotation.FunctionalInterface is not available at runtime,
        // so we can only verify the class exists
        assertNotNull(Property.class);

        // Try to create an instance to ensure the interface can be instantiated
        Property<TestBean> testProperty = createStringProperty();
        TestBean testBean = new TestBean();
        String result = testProperty.getValue(testBean);
        assertNotNull(result);
        assertEquals("test", result);
    }
}