package org.axonframework.common.infra;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilesystemStyleComponentDescriptorTest {

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
        FilesystemStyleComponentDescriptor descriptor = new FilesystemStyleComponentDescriptor();
        new Component().describeTo(descriptor);

        String out = descriptor.describe();
        assertNotNull(out);
        assertTrue(out.contains("value: 42"));
        assertTrue(out.contains("name: demo"));
    }

    @Test
    void symbolicLinkForSharedComponent() {
        FilesystemStyleComponentDescriptor descriptor = new FilesystemStyleComponentDescriptor();
        Component shared = new Component();
        descriptor.describeProperty("first", shared);
        descriptor.describeProperty("second", shared);

        String out = descriptor.describe();
        assertNotNull(out);
        assertTrue(out.contains("->"));
    }

    @Test
    void describeCollectionAndMapVariants() {
        FilesystemStyleComponentDescriptor descriptor = new FilesystemStyleComponentDescriptor();
        descriptor.describeProperty("nullCollection", (java.util.Collection<?>) null);
        descriptor.describeProperty("nullMap", (java.util.Map<?, ?>) null);

        String out = descriptor.describe();
        assertNotNull(out);
        assertTrue(out.contains("nullCollection: null"));
        assertTrue(out.contains("nullMap: null"));
    }
}