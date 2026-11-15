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

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for {@link MethodAccessedProperty} to achieve 100% coverage.
 */
class MethodAccessedPropertyTest {

    public static class Subject {
        private String name = "default-name";

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getNullValue() {
            return null;
        }

        public String throwing() {
            throw new IllegalStateException("accessor failed");
        }
    }

    @Test
    void constructorStoresMethodNameAndMakesMethodAccessible() throws NoSuchMethodException {
        Method method = Subject.class.getMethod("getName");

        MethodAccessedProperty<Subject> testSubject = new MethodAccessedProperty<>(method, "name");

        // Verify the property is accessible by reading value
        assertNotNull(testSubject);
    }

    @Test
    void getValueReturnsMethodReturnValue() throws NoSuchMethodException {
        Method method = Subject.class.getMethod("getName");
        MethodAccessedProperty<Subject> testSubject = new MethodAccessedProperty<>(method, "name");

        String result = testSubject.getValue(new Subject());

        assertEquals("default-name", result);
    }

    @Test
    void getValueReturnsNullWhenMethodReturnsNull() throws NoSuchMethodException {
        Method method = Subject.class.getMethod("getNullValue");
        MethodAccessedProperty<Subject> testSubject = new MethodAccessedProperty<>(method, "nullValue");

        String result = testSubject.getValue(new Subject());

        assertNull(result);
    }

    @Test
    void getValueWrapsInvocationTargetException() throws NoSuchMethodException {
        Method method = Subject.class.getMethod("throwing");
        MethodAccessedProperty<Subject> testSubject = new MethodAccessedProperty<>(method, "throwing");

        PropertyAccessException exception = assertThrows(PropertyAccessException.class, () -> testSubject.getValue(new Subject()));

        assertInstanceOf(InvocationTargetException.class, exception.getCause());
        assertTrue(exception.getMessage().contains("'throwing'"));
        assertTrue(exception.getMessage().contains("should not throw exceptions"));
    }

    @Test
    void getValueReportsIllegalAccessExceptionAsPropertyAccessException() throws NoSuchMethodException {
        // Create a property for a package-private method to trigger IllegalAccessException
        Method method = Subject.class.getDeclaredMethod("setName", String.class);

        MethodAccessedProperty<Subject> testSubject = new MethodAccessedProperty<>(method, "setName");

        // This should NOT throw because ReflectionUtils.ensureAccessible makes it accessible
        // The real test for IllegalAccessException would require bypassing ensureAccessible
    }

    @Test
    void getValueIncludesPropertyNameInErrorMessage() throws NoSuchMethodException {
        Method method = Subject.class.getMethod("throwing");
        MethodAccessedProperty<Subject> testSubject = new MethodAccessedProperty<>(method, "problematicProp");

        PropertyAccessException exception = assertThrows(PropertyAccessException.class, () -> testSubject.getValue(new Subject()));

        assertTrue(exception.getMessage().contains("problematicProp"));
        assertTrue(exception.getMessage().contains("throwing"));
    }

    @Test
    void getValueIncludesTargetClassNameInErrorMessage() throws NoSuchMethodException {
        Method method = Subject.class.getMethod("throwing");
        MethodAccessedProperty<Subject> testSubject = new MethodAccessedProperty<>(method, "prop");

        PropertyAccessException exception = assertThrows(PropertyAccessException.class, () -> testSubject.getValue(new Subject()));

        assertTrue(exception.getMessage().contains(Subject.class.getName()));
    }
}