package org.axonframework.common.configuration;

import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ConfigurationExtensionsTest {

    interface TestExtension extends ConfigurationExtension<ExtendedConfiguration> {
        int value();
    }

    private static TestExtension extension(int value) {
        return new TestExtension() {
            @Override
            public String name() {
                return "test";
            }

            @Override
            public void validate() {
            }

            @Override
            public int value() {
                return value;
            }

            @Override
            public void describeTo(org.axonframework.common.infra.ComponentDescriptor descriptor) {
            }
        };
    }

    @Test
    void extendAndRetrieve() {
        ConfigurationExtensions extensions = new ConfigurationExtensions(new ExtensibleConfigurer() {
            @Override
            public <T extends ConfigurationExtension<?>> ExtensibleConfigurer extend(Class<T> extensionType, Supplier<T> factory) {
                return this;
            }
        });

        extensions.extend(TestExtension.class, () -> extension(1));

        assertEquals(1, extensions.extension(TestExtension.class).value());
    }

    @Test
    void missingExtensionIsNull() {
        ExtensibleConfigurer parent = new ExtensibleConfigurer() {
            @Override
            public <T extends ConfigurationExtension<?>> ExtensibleConfigurer extend(Class<T> extensionType, Supplier<T> factory) {
                return this;
            }
        };
        ConfigurationExtensions extensions = new ConfigurationExtensions(parent);
        assertNull(extensions.extension(TestExtension.class));
    }

    @Test
    void extendReturnsParent() {
        ExtensibleConfigurer parent = new ExtensibleConfigurer() {
            @Override
            public <T extends ConfigurationExtension<?>> ExtensibleConfigurer extend(Class<T> extensionType, Supplier<T> factory) {
                return this;
            }
        };
        ConfigurationExtensions extensions = new ConfigurationExtensions(parent);

        assertSame(parent, extensions.extend(TestExtension.class, () -> extension(2)));
    }
}