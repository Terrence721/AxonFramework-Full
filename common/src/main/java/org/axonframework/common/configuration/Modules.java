/*
 * Copyright (c) 2026. Terrence Daniels
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.common.configuration;

import org.axonframework.common.annotation.Internal;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Internal helper that stores {@link Module} registrations and the {@link Configuration} results built for them,
 * for a {@link DefaultComponentRegistry}.
 * <p>
 * This class is not part of the public API. It exists purely to encapsulate module storage so that
 * {@link DefaultComponentRegistry} can delegate to it without duplicating logic. It intentionally does not own
 * the actual module-building orchestration (cloning the registry, creating a local configuration, invoking
 * {@link Module#build(Configuration, LifecycleRegistry)}) - that logic needs {@link DefaultComponentRegistry}'s
 * own decorator/enhancer state and stays there.
 *
 * @since 5.1.0
 */
@Internal
public class Modules {

    private final Map<String, Module> modules = new ConcurrentHashMap<>();
    private final Map<String, Configuration> configurations = new ConcurrentHashMap<>();

    /**
     * Registers the given {@code module}.
     *
     * @param module The module to register.
     * @throws DuplicateModuleRegistrationException If a module with the same name already exists.
     */
    public void register(Module module) {
        if (modules.containsKey(module.name())) {
            throw new DuplicateModuleRegistrationException(module);
        }
        modules.put(module.name(), module);
    }

    /**
     * Returns all registered modules.
     *
     * @return All registered modules.
     */
    public Collection<Module> registered() {
        return modules.values();
    }

    /**
     * Records the {@link Configuration} built for the module with the given {@code name}.
     *
     * @param name          The name of the module the given {@code configuration} was built for.
     * @param configuration The configuration built for the module.
     */
    public void recordBuildResult(String name, Configuration configuration) {
        configurations.put(name, configuration);
    }

    /**
     * Returns the {@link Configuration} results of all built modules.
     *
     * @return The configuration results of all built modules.
     */
    public List<Configuration> builtConfigurations() {
        return List.copyOf(configurations.values());
    }

    /**
     * Returns the {@link Configuration} built for the module with the given {@code name}, if present.
     *
     * @param name The name of the module to return the built configuration for.
     * @return The configuration built for the module with the given {@code name}, or an empty {@code Optional} if
     * no such module has been built yet.
     */
    public Optional<Configuration> builtConfiguration(String name) {
        return Optional.ofNullable(configurations.get(name));
    }
}
