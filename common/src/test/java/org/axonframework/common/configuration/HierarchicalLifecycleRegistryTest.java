package org.axonframework.common.configuration;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HierarchicalLifecycleRegistryTest {

    @Test
    void requiresParent() {
        assertThrows(NullPointerException.class,
                () -> HierarchicalLifecycleRegistry.build(null, registry -> fakeConfiguration()));
    }

    @Test
    void buildsChildConfiguration() {
        AtomicBoolean timeoutCalled = new AtomicBoolean(false);
        LifecycleRegistry parent = fakeLifecycleRegistry(timeoutCalled);

        Configuration expected = fakeConfiguration();

        Configuration result = HierarchicalLifecycleRegistry.build(parent, registry -> {
            registry.registerLifecyclePhaseTimeout(2, TimeUnit.SECONDS);
            return expected;
        });

        assertSame(expected, result);
        assertTrue(timeoutCalled.get());
    }

    private static Configuration fakeConfiguration() {
        return (Configuration) Proxy.newProxyInstance(
                HierarchicalLifecycleRegistryTest.class.getClassLoader(),
                new Class[]{Configuration.class},
                (proxy, method, args) -> {
                    if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
                    if ("equals".equals(method.getName()) && args != null && args.length == 1) return proxy == args[0];
                    if ("toString".equals(method.getName())) return "FakeConfiguration";
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) return false;
                    if (returnType == int.class) return 0;
                    if (returnType == long.class) return 0L;
                    return null;
                });
    }

    private static LifecycleRegistry fakeLifecycleRegistry(AtomicBoolean timeoutCalled) {
        return (LifecycleRegistry) Proxy.newProxyInstance(
                HierarchicalLifecycleRegistryTest.class.getClassLoader(),
                new Class[]{LifecycleRegistry.class},
                (proxy, method, args) -> {
                    if ("registerLifecyclePhaseTimeout".equals(method.getName())) {
                        timeoutCalled.set(true);
                        return proxy;
                    }
                    if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
                    if ("equals".equals(method.getName()) && args != null && args.length == 1) return proxy == args[0];
                    if ("toString".equals(method.getName())) return "FakeLifecycleRegistry";
                    Class<?> returnType = method.getReturnType();
                    if (LifecycleRegistry.class.isAssignableFrom(returnType)) return proxy;
                    if (returnType == boolean.class) return false;
                    if (returnType == int.class) return 0;
                    if (returnType == long.class) return 0L;
                    return null;
                });
    }
}