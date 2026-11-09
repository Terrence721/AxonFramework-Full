package org.axonframework.common.infra;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JacksonComponentDescriptorTest {

    static class Component implements DescribableComponent {
        @Override
        public void describeTo(ComponentDescriptor descriptor) {
            descriptor.describeProperty("value", (Object) 42);
            descriptor.describeProperty("name", (Object) "demo");
            descriptor.describeProperty("bool", (Object) Boolean.TRUE);
            descriptor.describeProperty("nul", (Object) null);
            descriptor.describeProperty("list", List.of(1, 2));
            descriptor.describeProperty("map", Map.of("k", "v"));
        }
    }

    @Test
    void describeNestedComponent() {
        JacksonComponentDescriptor descriptor = new JacksonComponentDescriptor();
        new Component().describeTo(descriptor);

        String out = descriptor.describe();
        assertNotNull(out);
        assertTrue(out.contains("value"));
        assertTrue(out.contains("demo"));
    }

    @Test
    void symbolicLinkForSharedComponent() {
        JacksonComponentDescriptor descriptor = new JacksonComponentDescriptor();
        Component shared = new Component();
        descriptor.describeProperty("first", shared);
        descriptor.describeProperty("second", shared);

        String out = descriptor.describe();
        assertNotNull(out);
        assertTrue(out.contains("$ref"));
    }

    @Test
    void describeNullCollectionAndMap() {
        JacksonComponentDescriptor descriptor = new JacksonComponentDescriptor();
        descriptor.describeProperty("nullCollection", (java.util.Collection<?>) null);
        descriptor.describeProperty("nullMap", (java.util.Map<?, ?>) null);

        String out = descriptor.describe();
        assertNotNull(out);
    }
}