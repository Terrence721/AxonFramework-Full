package org.axonframework.common.configuration;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractComponentTest {

    @Test
    void createAndResolve() {
        InstantiatedComponentDefinition<String> component =
                new InstantiatedComponentDefinition<>(new Component.Identifier<>(String.class, "cmp"), "value");

        assertSame(component, component.createComponent());
        assertEquals("value", component.resolve(fakeConfiguration()));
        assertEquals(String.class, component.rawType());
        assertEquals("cmp", component.name());
        assertNotNull(component.identifier());
    }

    @Test
    void lifecycleHandlersRegisteredAndInitialized() {
        InstantiatedComponentDefinition<String> component =
                new InstantiatedComponentDefinition<>(new Component.Identifier<>(String.class, "cmp"), "value");

        component.onStart(0, (configuration, instance) -> {
        });
        component.onShutdown(0, (configuration, instance) -> {
        });

        LifecycleRegistry registry = new LifecycleRegistry() {
            @Override
            public LifecycleRegistry registerLifecyclePhaseTimeout(long timeout, java.util.concurrent.TimeUnit timeUnit) {
                return this;
            }

            @Override
            public LifecycleRegistry onStart(int phase, org.axonframework.common.configuration.LifecycleHandler startHandler) {
                return this;
            }

            @Override
            public LifecycleRegistry onShutdown(int phase, org.axonframework.common.configuration.LifecycleHandler shutdownHandler) {
                return this;
            }
        };

        component.initLifecycle(fakeConfiguration(), registry);
        assertTrue(component.isInitialized());
        // idempotent
        component.initLifecycle(fakeConfiguration(), registry);

        assertThrows(NullPointerException.class, () -> component.initLifecycle(null, null));
    }

    private static Configuration fakeConfiguration() {
        return (Configuration) Proxy.newProxyInstance(
                AbstractComponentTest.class.getClassLoader(),
                new Class[]{Configuration.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "equals":
                            return proxy == args[0];
                        case "toString":
                            return "FakeConfiguration@" + System.identityHashCode(proxy);
                        default:
                            break;
                    }
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) {
                        return false;
                    }
                    if (returnType == int.class) {
                        return 0;
                    }
                    if (returnType == long.class) {
                        return 0L;
                    }
                    if (returnType == double.class) {
                        return 0.0;
                    }
                    if (returnType == float.class) {
                        return 0.0f;
                    }
                    if (returnType == char.class) {
                        return (char) 0;
                    }
                    return null;
                });
    }

    @Test
    void nullArgumentsThrow() {
        assertThrows(NullPointerException.class, () -> new InstantiatedComponentDefinition<>(null, "value"));
        InstantiatedComponentDefinition<String> component =
                new InstantiatedComponentDefinition<>(new Component.Identifier<>(String.class, "cmp"), "value");
        assertThrows(NullPointerException.class, () -> component.resolve(null)); // not null-safe? Actually null allowed? doResolve ignores null
    }
}