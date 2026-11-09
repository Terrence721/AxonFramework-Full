package org.axonframework.common.configuration;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LazyInitializedComponentDefinitionTest {

    @Test
    void lazilyBuildsOnce() {
        AtomicInteger builds = new AtomicInteger();
        Component.Identifier<String> identifier = new Component.Identifier<>(String.class, "lazy");
        LazyInitializedComponentDefinition<String, String> definition =
                new LazyInitializedComponentDefinition<>(identifier, config -> "value-" + builds.incrementAndGet());

        assertTrue(!definition.isInstantiated());
        assertEquals("value-1", definition.resolve(fakeConfiguration()));
        assertEquals("value-1", definition.resolve(fakeConfiguration()));
        assertTrue(definition.isInstantiated());
        assertEquals(1, builds.get());
    }

    @Test
    void rejectsNullBuilder() {
        assertThrows(NullPointerException.class,
                () -> new LazyInitializedComponentDefinition<>(new Component.Identifier<>(String.class, "lazy"), null));
    }

    private static Configuration fakeConfiguration() {
        return (Configuration) Proxy.newProxyInstance(
                LazyInitializedComponentDefinitionTest.class.getClassLoader(),
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
}