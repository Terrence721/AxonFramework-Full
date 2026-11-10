package org.axonframework.common.configuration;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecoratedComponentTest {

    @Test
    void resolveDecoratesDelegate() {
        Component<String> delegate = new InstantiatedComponentDefinition<>(
                new Component.Identifier<>(String.class, "cmp"), "base");
        ComponentDecorator<String, String> decorator = (config, name, value) -> "decorated:" + value;

        DecoratedComponent<String, String> decorated =
                new DecoratedComponent<>(delegate, decorator, List.of(), List.of());

        assertEquals("decorated:base", decorated.resolve(fakeConfiguration()));
        assertTrue(decorated.isInstantiated());
    }

    @Test
    void resolveThrowsWhenDecoratedTypeIsIncompatible() {
        Component<String> delegate = new InstantiatedComponentDefinition<>(
                new Component.Identifier<>(String.class, "cmp"), "base");
        ComponentDecorator<String, Object> incompatibleDecorator = (config, name, value) -> 42;
        @SuppressWarnings("unchecked")
        ComponentDecorator<String, String> decorator =
                (ComponentDecorator<String, String>) (ComponentDecorator<?, ?>) incompatibleDecorator;

        DecoratedComponent<String, String> decorated =
                new DecoratedComponent<>(delegate, decorator, List.of(), List.of());

        assertThrows(ClassCastException.class, () -> decorated.resolve(fakeConfiguration()));
    }

    @SuppressWarnings("unchecked")
    private static Configuration fakeConfiguration() {
        return (Configuration) Proxy.newProxyInstance(
                DecoratedComponentTest.class.getClassLoader(),
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