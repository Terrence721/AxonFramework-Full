package org.axonframework.common.configuration;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BaseModuleTest {

    static class TestModule extends BaseModule<TestModule> {
        TestModule(String name) {
            super(name);
        }
    }

    @Test
    void exposesName() {
        assertEquals("mod", new TestModule("mod").name());
    }

    @Test
    void rejectsEmptyName() {
        assertThrows(IllegalArgumentException.class, () -> new TestModule(""));
    }

    @Test
    void componentRegistryActionIsRejectedAfterBuild() {
        DefaultComponentRegistry parentRegistry = new DefaultComponentRegistry();
        Configuration parent = parentRegistry.build(fakeLifecycleRegistry());
        TestModule module = new TestModule("mod");

        Configuration child = module.build(parent, fakeLifecycleRegistry());

        assertNotNull(child);
        assertThrows(IllegalStateException.class, () -> module.componentRegistry(registry -> {
        }));
    }

    @Test
    void rejectsNullRegistryAction() {
        assertThrows(NullPointerException.class,
                () -> new TestModule("mod").componentRegistry(null));
    }

    @Test
    void returnsSameModuleForFluentRegistration() {
        TestModule module = new TestModule("mod");
        assertSame(module, module.componentRegistry(registry -> {
        }));
    }

    private static LifecycleRegistry fakeLifecycleRegistry() {
        return (LifecycleRegistry) Proxy.newProxyInstance(
                BaseModuleTest.class.getClassLoader(),
                new Class[]{LifecycleRegistry.class},
                (proxy, method, args) -> {
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(method.getName()) && args != null && args.length == 1) {
                        return proxy == args[0];
                    }
                    if ("toString".equals(method.getName())) {
                        return "FakeLifecycleRegistry";
                    }
                    if (LifecycleRegistry.class.isAssignableFrom(method.getReturnType())) {
                        return proxy;
                    }
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) return false;
                    if (returnType == int.class) return 0;
                    if (returnType == long.class) return 0L;
                    return null;
                });
    }
}