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

package org.axonframework.common.infra;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for {@link ComponentDescriptor} interface to achieve 100% coverage.
 */
class ComponentDescriptorTest {

    /**
     * Concrete implementation of ComponentDescriptor for testing interface methods.
     * Uses type-safe casting to resolve overload ambiguity.
     */
    private static class TestDescriptor implements ComponentDescriptor {

        private final java.util.List<String> calls = new java.util.ArrayList<>();

        @Override
        public void describeProperty(String name, Object object) {
            // Type-specific handling to avoid overload ambiguity
            if (object instanceof String) {
                calls.add("describeProperty(name=\"" + name + "\", value=\"" + object + "\")");
            } else if (object instanceof Long) {
                calls.add("describeProperty(name=\"" + name + "\", value=" + object + ")");
            } else if (object instanceof Boolean) {
                calls.add("describeProperty(name=\"" + name + "\", value=" + object + ")");
            } else if (object instanceof Collection) {
                calls.add("describeProperty(name=\"" + name + "\", collection=" + object + ")");
            } else if (object instanceof Map) {
                calls.add("describeProperty(name=\"" + name + "\", map=" + object + ")");
            } else {
                calls.add("describeProperty(name=\"" + name + "\", object=" + object + ")");
            }
        }

        @Override
        public void describeProperty(String name, Collection<?> collection) {
            calls.add("describeProperty(name=\"" + name + "\", collection=" + collection + ")");
        }

        @Override
        public void describeProperty(String name, Map<?, ?> map) {
            calls.add("describeProperty(name=\"" + name + "\", map=" + map + ")");
        }

        @Override
               public void describeProperty(String name, String value) {
            calls.add("describeProperty(name=\"" + name + "\", value=\"" + value + "\")");
        }

        @Override
        public void describeProperty(String name, Long value) {
            calls.add("describeProperty(name=\"" + name + "\", value=" + value + ")");
        }

        @Override
        public void describeProperty(String name, Boolean value) {
            calls.add("describeProperty(name=\"" + name + "\", value=" + value + ")");
        }

        public java.util.List<String> getCalls() {
            return new java.util.ArrayList<>(calls);
        }
    }

    @Test
    void testDescribePropertyWithString() {
        TestDescriptor descriptor = new TestDescriptor();
        descriptor.describeProperty("myString", "hello");

        assertEquals("describeProperty(name=\"myString\", value=\"hello\")", descriptor.getCalls().get(0));
    }

    @Test
    void testDescribePropertyWithLong() {
        TestDescriptor descriptor = new TestDescriptor();
        descriptor.describeProperty("myNumber", 123L);

        assertEquals("describeProperty(name=\"myNumber\", value=123)", descriptor.getCalls().get(0));
    }

    @Test
    void testDescribePropertyWithBoolean() {
        TestDescriptor descriptor = new TestDescriptor();
        descriptor.describeProperty("myBool", true);

        assertEquals("describeProperty(name=\"myBool\", value=true)", descriptor.getCalls().get(0));
    }

    @Test
    void testDescribePropertyWithIntegerObject() {
        TestDescriptor descriptor = new TestDescriptor();
        descriptor.describeProperty("myInt", 42);

        assertEquals("describeProperty(name=\"myInt\", object=42)", descriptor.getCalls().get(0));
    }

    @Test
    void testDescribePropertyWithStringObject() {
        TestDescriptor descriptor = new TestDescriptor();
        descriptor.describeProperty("myStringObj", "test");

        assertEquals("describeProperty(name=\"myStringObj\", value=\"test\")", descriptor.getCalls().get(0));
    }

    @Test
    void testDescribePropertyWithLongObject() {
        TestDescriptor descriptor = new TestDescriptor();
        descriptor.describeProperty("myLongObj", 123L);

        assertEquals("describeProperty(name=\"myLongObj\", value=123)", descriptor.getCalls().get(0));
    }

    @Test
    void testDescribePropertyWithBooleanObject() {
        TestDescriptor descriptor = new TestDescriptor();
        descriptor.describeProperty("myBoolObj", true);

        assertEquals("describeProperty(name=\"myBoolObj\", value=true)", descriptor.getCalls().get(0));
    }

    @Test
    void testDescribePropertyWithNullObject() {
        TestDescriptor descriptor = new TestDescriptor();
        descriptor.describeProperty("nullObj", (Object) null);

        assertEquals("describeProperty(name=\"nullObj\", object=null)", descriptor.getCalls().get(0));
    }

    @Test
    void testDescribePropertyWithCollection() {
        TestDescriptor descriptor = new TestDescriptor();
        descriptor.describeProperty("myList", List.of(1, 2, 3));

        assertEquals("describeProperty(name=\"myList\", collection=[1, 2, 3])", descriptor.getCalls().get(0));
    }

    @Test
    void testDescribePropertyWithMap() {
        TestDescriptor descriptor = new TestDescriptor();
        descriptor.describeProperty("myMap", Map.of("key", "value"));

        assertEquals("describeProperty(name=\"myMap\", map={key=value})", descriptor.getCalls().get(0));
    }

    @Test
    void testDescribePropertyWithStringValue() {
        TestDescriptor descriptor = new TestDescriptor();
        descriptor.describeProperty("myString", "hello");

        assertEquals("describeProperty(name=\"myString\", value=\"hello\")", descriptor.getCalls().get(0));
    }

    @Test
    void testDescribeWrapperOfWithObject() {
        TestDescriptor descriptor = new TestDescriptor();
        descriptor.describeWrapperOf("wrapperObject");

        assertEquals("describeProperty(name=\"delegate\", value=\"wrapperObject\")", descriptor.getCalls().get(0));
    }

    @Test
    void testDescribeWrapperOfWithCollection() {
        TestDescriptor descriptor = new TestDescriptor();
        descriptor.describeWrapperOf(List.of(1, 2, 3));

        assertEquals("describeProperty(name=\"delegate\", collection=[1, 2, 3])", descriptor.getCalls().get(0));
    }

    @Test
    void testDescribeWrapperOfWithMap() {
        TestDescriptor descriptor = new TestDescriptor();
        descriptor.describeWrapperOf(Map.of("key", "value"));

        assertEquals("describeProperty(name=\"delegate\", map={key=value})", descriptor.getCalls().get(0));
    }

    @Test
    void testDescribeWrapperOfWithLong() {
        TestDescriptor descriptor = new TestDescriptor();
        descriptor.describeWrapperOf(123L);

        assertEquals("describeProperty(name=\"delegate\", value=123)", descriptor.getCalls().get(0));
    }

    @Test
    void testDescribeWrapperOfWithBoolean() {
        TestDescriptor descriptor = new TestDescriptor();
        descriptor.describeWrapperOf(true);

        assertEquals("describeProperty(name=\"delegate\", value=true)", descriptor.getCalls().get(0));
    }

    @Test
    void testDescribeWrapperOfWithNull() {
        TestDescriptor descriptor = new TestDescriptor();
        descriptor.describeWrapperOf(null);

        assertEquals("describeProperty(name=\"delegate\", object=null)", descriptor.getCalls().get(0));
    }

    @Test
    void testDescribePropertyWithMultipleDifferentTypes() {
        TestDescriptor descriptor = new TestDescriptor();
        descriptor.describeProperty("string", "value1");
        descriptor.describeProperty("number", 42);
        descriptor.describeProperty("bool", true);
        descriptor.describeProperty("list", List.of(1, 2));
        descriptor.describeProperty("map", Map.of("k", "v"));
        descriptor.describeProperty("null", (Object) null);

        java.util.List<String> calls = descriptor.getCalls();
        assertEquals("describeProperty(name=\"string\", value=\"value1\")", calls.get(0));
        assertEquals("describeProperty(name=\"number\", object=42)", calls.get(1));
        assertEquals("describeProperty(name=\"bool\", value=true)", calls.get(2));
        assertEquals("describeProperty(name=\"list\", collection=[1, 2])", calls.get(3));
        assertEquals("describeProperty(name=\"map\", map={k=v})", calls.get(4));
        assertEquals("describeProperty(name=\"null\", object=null)", calls.get(5));
    }

    @Test
    void testDescribePropertyWithAllPrimitiveWrapperTypes() {
        TestDescriptor descriptor = new TestDescriptor();
        descriptor.describeProperty("byte", Byte.valueOf((byte) 1));
        descriptor.describeProperty("short", Short.valueOf((short) 2));
        descriptor.describeProperty("int", Integer.valueOf(3));
        descriptor.describeProperty("long", Long.valueOf(4L));
        descriptor.describeProperty("float", Float.valueOf(5.0f));
        descriptor.describeProperty("double", Double.valueOf(6.0));
        descriptor.describeProperty("boolean", Boolean.TRUE);
        descriptor.describeProperty("char", Character.valueOf('a'));

        java.util.List<String> calls = descriptor.getCalls();
        assertEquals("describeProperty(name=\"byte\", object=1)", calls.get(0));
        assertEquals("describeProperty(name=\"short\", object=2)", calls.get(1));
        assertEquals("describeProperty(name=\"int\", object=3)", calls.get(2));
        assertEquals("describeProperty(name=\"long\", value=4)", calls.get(3));
        assertEquals("describeProperty(name=\"float\", object=5.0)", calls.get(4));
        assertEquals("describeProperty(name=\"double\", object=6.0)", calls.get(5));
        assertEquals("describeProperty(name=\"boolean\", value=true)", calls.get(6));
        assertEquals("describeProperty(name=\"char\", object=a)", calls.get(7));
    }

    @Test
    void testDescribePropertyWithCustomObject() {
        class CustomObject {
            @Override
            public String toString() {
                return "Custom";
            }
        }

        TestDescriptor descriptor = new TestDescriptor();
        descriptor.describeProperty("custom", new CustomObject());

        String call = descriptor.getCalls().get(0);
        assertTrue(call.startsWith("describeProperty(name=\"custom\", object="));
        assertTrue(call.endsWith(")"));
    }

    @Test
    void testDescribePropertyWithInterface() {
        class MyInterface implements java.io.Serializable {
            @Override
            public String toString() {
                return "MyInterface";
            }
        }

        TestDescriptor descriptor = new TestDescriptor();
        descriptor.describeProperty("interfaceObj", new MyInterface());

        String call = descriptor.getCalls().get(0);
        assertTrue(call.startsWith("describeProperty(name=\"interfaceObj\", object="));
        assertTrue(call.endsWith(")"));
    }

    @Test
    void testDescribePropertyWithEnum() {
        TestDescriptor descriptor = new TestDescriptor();
        descriptor.describeProperty("enum", java.time.DayOfWeek.MONDAY);

        assertEquals("describeProperty(name=\"enum\", object=MONDAY)", descriptor.getCalls().get(0));
    }

    @Test
    void testDescribePropertyWithLocalDate() {
        TestDescriptor descriptor = new TestDescriptor();
        descriptor.describeProperty("date", java.time.LocalDate.of(2024, 1, 1));

        String call = descriptor.getCalls().get(0);
        assertTrue(call.startsWith("describeProperty(name=\"date\", object="));
        assertTrue(call.endsWith(")"));
    }

    @Test
    void testDescribePropertyWithRecord() {
        record MyRecord(String name, int value) {}

        TestDescriptor descriptor = new TestDescriptor();
        descriptor.describeProperty("record", new MyRecord("test", 42));

        String call = descriptor.getCalls().get(0);
        assertTrue(call.startsWith("describeProperty(name=\"record\", object="));
        assertTrue(call.endsWith(")"));
    }

    @Test
    void testDescribePropertyWithStringArray() {
        TestDescriptor descriptor = new TestDescriptor();
        String[] array = {"a", "b", "c"};
        descriptor.describeProperty("array", array);

        String call = descriptor.getCalls().get(0);
        assertTrue(call.startsWith("describeProperty(name=\"array\", object="));
        assertTrue(call.endsWith(")"));
    }

    @Test
    void testDescribePropertyWithPrimitiveArray() {
        TestDescriptor descriptor = new TestDescriptor();
        int[] array = {1, 2, 3};
        descriptor.describeProperty("array", array);

        String call = descriptor.getCalls().get(0);
        assertTrue(call.startsWith("describeProperty(name=\"array\", object="));
        assertTrue(call.endsWith(")"));
    }

    @Test
    void testDescribePropertyWithLambda() {
        TestDescriptor descriptor = new TestDescriptor();
        Runnable lambda = () -> System.out.println("test");
        descriptor.describeProperty("lambda", lambda);

        String call = descriptor.getCalls().get(0);
        assertTrue(call.startsWith("describeProperty(name=\"lambda\", object="));
        assertTrue(call.endsWith(")"));
    }

    @Test
    void testDescribePropertyWithAnonymousClass() {
        TestDescriptor descriptor = new TestDescriptor();
        Runnable anonymous = new Runnable() {
            @Override
            public void run() {
                /* empty */
            }
        };
        descriptor.describeProperty("anonymous", anonymous);

        String call = descriptor.getCalls().get(0);
        assertTrue(call.startsWith("describeProperty(name=\"anonymous\", object="));
        assertTrue(call.endsWith(")"));
    }

    @Test
    void testDescribeWrapperOfWithComplexObject() {
        class ComplexObject {
            public String field = "complex";
        }

        TestDescriptor descriptor = new TestDescriptor();
        descriptor.describeWrapperOf(new ComplexObject());

        String call = descriptor.getCalls().get(0);
        assertTrue(call.startsWith("describeProperty(name=\"delegate\", object="));
        assertTrue(call.endsWith(")"));
    }

    @Test
    void testDescribePropertyWithAllTypesInSequence() {
        TestDescriptor descriptor = new TestDescriptor();
        descriptor.describeProperty("string", "value1");
        descriptor.describeProperty("int", 42);
        descriptor.describeProperty("bool", true);
        descriptor.describeProperty("long", 123L);
        descriptor.describeProperty("list", List.of(1, 2));
        descriptor.describeProperty("map", Map.of("k", "v"));
        descriptor.describeProperty("null", (Object) null);
        descriptor.describeWrapperOf("wrapper");

        java.util.List<String> calls = descriptor.getCalls();
        assertEquals("describeProperty(name=\"string\", value=\"value1\")", calls.get(0));
        assertEquals("describeProperty(name=\"int\", object=42)", calls.get(1));
        assertEquals("describeProperty(name=\"bool\", value=true)", calls.get(2));
        assertEquals("describeProperty(name=\"long\", value=123)", calls.get(3));
        assertEquals("describeProperty(name=\"list\", collection=[1, 2])", calls.get(4));
        assertEquals("describeProperty(name=\"map\", map={k=v})", calls.get(5));
        assertEquals("describeProperty(name=\"null\", object=null)", calls.get(6));
        assertEquals("describeProperty(name=\"delegate\", value=\"wrapper\")", calls.get(7));
    }
}