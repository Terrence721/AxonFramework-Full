package org.axonframework.common.configuration;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DefaultComponentRegistryTest {

    @Test
    void registerAndBuild() {
        DefaultComponentRegistry registry = new DefaultComponentRegistry();
        registry.registerComponent(ComponentDefinition.ofType(String.class).withInstance("value"));
        registry.setOverridePolicy(OverridePolicy.WARN);

        LifecycleRegistry lifecycleRegistry = fakeLifecycleRegistry();
        Configuration configuration = registry.build(lifecycleRegistry);

        assertEquals("value", configuration.getOptionalComponent(String.class, null).orElseThrow());
        org.junit.jupiter.api.Assertions.assertTrue(registry.hasComponent(String.class, null, SearchScope.CURRENT));
    }

    @Test
    void componentLookupPaths() {
        DefaultComponentRegistry registry = new DefaultComponentRegistry();
        LifecycleRegistry lifecycleRegistry = fakeLifecycleRegistry();
        Configuration configuration = registry.build(lifecycleRegistry);

        org.junit.jupiter.api.Assertions.assertTrue(configuration.getOptionalComponent(String.class, null).isEmpty());
        assertEquals("default", configuration.getComponent(String.class, null, () -> "default"));
        // registry self-registers as ComponentRegistry, so String components may remain empty
        org.junit.jupiter.api.Assertions.assertTrue(configuration.getModuleConfigurations().isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(configuration.getModuleConfiguration("missing").isEmpty());
    }

    @Test
    void hasComponentScopes() {
        DefaultComponentRegistry registry = new DefaultComponentRegistry();
        registry.registerComponent(ComponentDefinition.ofType(String.class).withInstance("value"));

        org.junit.jupiter.api.Assertions.assertTrue(registry.hasComponent(String.class, null, SearchScope.CURRENT));
        assertFalse(registry.hasComponent(Integer.class, null, SearchScope.CURRENT));
    }

    @Test
    void disableEnhancerByClassName() {
        DefaultComponentRegistry registry = new DefaultComponentRegistry();
        ComponentRegistry result = registry.disableEnhancer("does.not.Exist");
        org.junit.jupiter.api.Assertions.assertSame(registry, result);
    }

    private static LifecycleRegistry fakeLifecycleRegistry() {
        return (LifecycleRegistry) Proxy.newProxyInstance(
                DefaultComponentRegistryTest.class.getClassLoader(),
                new Class[]{LifecycleRegistry.class},
                (proxy, method, args) -> {
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(method.getName()) && args != null && args.length == 1) {
                        return proxy == args[0];
                    }
                    if ("toString".equals(method.getName())) {
                        return "FakeLifecycleRegistry@" + System.identityHashCode(proxy);
                    }
                    if (LifecycleRegistry.class.isAssignableFrom(method.getReturnType())) {
                        return proxy;
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
                    return null;
                });
    }
}