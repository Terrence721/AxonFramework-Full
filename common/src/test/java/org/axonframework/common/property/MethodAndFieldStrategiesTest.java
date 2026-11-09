/*
 * Copyright (c) 2026. Terrence Daniels
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

package org.axonframework.common.property;

import org.axonframework.common.AxonConfigurationException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link UniformPropertyAccessStrategy}, the {@link DirectPropertyAccessStrategy}, the
 * {@link MethodAccessedProperty}, the {@link DirectlyAccessedProperty} and the {@link PropertyAccessException}.
 */
class MethodAndFieldStrategiesTest {

    public static class Subject {

        public String visible = "visible";
        public String empty = null;
        private final String hidden = "hidden";

        public String name() {
            return "uniform-name";
        }

        public String withArgument(String argument) {
            return argument;
        }

        public void returnsNothing() {
        }

        public String failing() {
            throw new IllegalStateException("accessor failed");
        }

        public String hidden() {
            return hidden;
        }
    }

    // --- UniformPropertyAccessStrategy ---

    @Test
    void uniformStrategyUsesPropertyNameAsAccessorName() {
        Property<Subject> property = new UniformPropertyAccessStrategy().propertyFor(Subject.class, "name");

        assertNotNull(property);
        assertEquals("uniform-name", property.getValue(new Subject()));
    }

    @Test
    void uniformStrategyIgnoresMethodsWithArgumentsOrVoidReturnType() {
        UniformPropertyAccessStrategy testSubject = new UniformPropertyAccessStrategy();

        assertNull(testSubject.propertyFor(Subject.class, "withArgument"));
        assertNull(testSubject.propertyFor(Subject.class, "returnsNothing"));
        assertNull(testSubject.propertyFor(Subject.class, "unknown"));
    }

    @Test
    void uniformStrategyHasPriorityBetweenBeanAndDirectAccess() {
        PropertyAccessStrategy bean = new BeanPropertyAccessStrategy();
        PropertyAccessStrategy uniform = new UniformPropertyAccessStrategy();
        PropertyAccessStrategy direct = new DirectPropertyAccessStrategy();

        assertTrue(bean.compareTo(uniform) < 0);
        assertTrue(uniform.compareTo(direct) < 0);
    }

    // --- DirectPropertyAccessStrategy ---

    @Test
    void directStrategyFindsPublicFields() {
        Property<Subject> property = new DirectPropertyAccessStrategy().propertyFor(Subject.class, "visible");

        assertNotNull(property);
        assertEquals("visible", property.getValue(new Subject()));
    }

    @Test
    void directStrategyReadsNullFieldValuesAsNull() {
        Property<Subject> property = new DirectPropertyAccessStrategy().propertyFor(Subject.class, "empty");

        assertNotNull(property);
        assertNull(property.getValue(new Subject()));
    }

    @Test
    void directStrategyIgnoresNonPublicAndUnknownFields() {
        DirectPropertyAccessStrategy testSubject = new DirectPropertyAccessStrategy();

        assertNull(testSubject.propertyFor(Subject.class, "hidden"));
        assertNull(testSubject.propertyFor(Subject.class, "unknown"));
    }

    // --- accessed properties ---

    @Test
    void methodAccessedPropertyReturnsTheMethodResult() throws NoSuchMethodException {
        Method method = Subject.class.getMethod("name");

        MethodAccessedProperty<Subject> testSubject = new MethodAccessedProperty<>(method, "name");

        assertEquals("uniform-name", testSubject.getValue(new Subject()));
    }

    @Test
    void methodAccessedPropertyWrapsExceptionsThrownByTheMethod() throws NoSuchMethodException {
        MethodAccessedProperty<Subject> testSubject =
                new MethodAccessedProperty<>(Subject.class.getMethod("failing"), "failing");

        PropertyAccessException exception =
                assertThrows(PropertyAccessException.class, () -> testSubject.getValue(new Subject()));

        assertInstanceOf(InvocationTargetException.class, exception.getCause());
        assertTrue(exception.getMessage().contains("'failing'"));
        assertTrue(exception.getMessage().contains("should not throw exceptions"));
    }

    @Test
    void directlyAccessedPropertyReturnsTheFieldValue() throws NoSuchFieldException {
        DirectlyAccessedProperty<Subject> testSubject =
                new DirectlyAccessedProperty<>(Subject.class.getField("visible"), "visible");

        assertEquals("visible", testSubject.getValue(new Subject()));
    }

    @Test
    void directlyAccessedPropertyReportsInaccessibleFieldsAsPropertyAccessException() throws NoSuchFieldException {
        Field privateField = Subject.class.getDeclaredField("hidden");
        DirectlyAccessedProperty<Subject> testSubject = new DirectlyAccessedProperty<>(privateField, "hidden");

        PropertyAccessException exception =
                assertThrows(PropertyAccessException.class, () -> testSubject.getValue(new Subject()));

        assertInstanceOf(IllegalAccessException.class, exception.getCause());
        assertTrue(exception.getMessage().contains("Property should be accessible"));
    }

    // --- PropertyAccessException ---

    @Test
    void propertyAccessExceptionRetainsMessageAndCauseAndIsAConfigurationException() {
        IllegalStateException cause = new IllegalStateException("root");

        PropertyAccessException testSubject = new PropertyAccessException("could not read", cause);

        assertEquals("could not read", testSubject.getMessage());
        assertSame(cause, testSubject.getCause());
        assertInstanceOf(AxonConfigurationException.class, testSubject);
    }

    @Test
    void propertyAccessExceptionAcceptsNullCause() {
        assertNull(new PropertyAccessException("could not read", null).getCause());
    }
}
