package org.axonframework.common.configuration;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModulesTest {

    private static Module module(String name) {
        return new Module() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public Configuration build(Configuration parent, LifecycleRegistry lifecycleRegistry) {
                return parent;
            }
        };
    }

    @Test
    void registerAndRetrieveRegistered() {
        Modules modules = new Modules();
        Module module = module("mod");

        modules.register(module);

        assertEquals(1, modules.registered().size());
        assertTrue(modules.registered().contains(module));
    }

    @Test
    void duplicateRegistrationThrows() {
        Modules modules = new Modules();
        modules.register(module("mod"));

        assertThrows(DuplicateModuleRegistrationException.class, () -> modules.register(module("mod")));
    }

    @Test
    void recordsBuiltConfigurations() {
        Modules modules = new Modules();
        Configuration configuration = new Configuration() {
            @Override
            public <C> Optional<C> getOptionalComponent(Class<C> type, String name) {
                return Optional.empty();
            }

            @Override
            public <C> Optional<C> getOptionalComponent(org.axonframework.common.TypeReference<C> typeReference, String name) {
                return Optional.empty();
            }

            @Override
            public <C> C getComponent(Class<C> type, String name, Supplier<C> defaultImpl) {
                return defaultImpl.get();
            }

            @Override
            public List<Configuration> getModuleConfigurations() {
                return List.of();
            }

            @Override
            public Optional<Configuration> getModuleConfiguration(String name) {
                return Optional.empty();
            }

            @Override
            public Configuration getParent() {
                return null;
            }

            @Override
            public <C> Map<String, C> getComponents(Class<C> type) {
                return Map.of();
            }

            @Override
            public void describeTo(org.axonframework.common.infra.ComponentDescriptor descriptor) {
            }
        };

        modules.recordBuildResult("mod", configuration);

        assertEquals(1, modules.builtConfigurations().size());
        assertEquals(configuration, modules.builtConfiguration("mod").orElseThrow());
    }
}