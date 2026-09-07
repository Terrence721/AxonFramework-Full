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

/**
 * Additional, less commonly needed {@link ComponentRegistry} configuration operations: registering a
 * {@link ComponentFactory} and setting the registry's {@link OverridePolicy}.
 * <p>
 * Split out of {@link ComponentRegistry} itself so that the interface most {@link ConfigurationEnhancer}/
 * {@link Module} authors work with stays focused on its heavily-used surface (registering components,
 * decorators, enhancers, and modules). These two operations remain real, intentional, supported features - just
 * rare ones - so this is a plain interface, not one marked {@link org.axonframework.common.annotation.Internal}.
 * {@link DefaultComponentRegistry} implements both interfaces.
 *
 * @since 5.1.0
 */
public interface ConfigurableComponentRegistry {

    /**
     * Registers a {@link ComponentFactory} with this registry.
     * <p>
     * If the {@link Configuration} that will contain this registry <b>does not</b> have a component for a given
     * {@code Class} and name combination, it will consult all registered component factories. Only if a given
     * {@code factory} can produce the {@link ComponentFactory#forType() requested type} will
     * {@link ComponentFactory#construct(String, Configuration)} be invoked. When the {@code factory} decides to
     * construct a new component, it will be stored in the {@code Configuration} for future reference to ensure it's not
     * constructed again.
     *
     * @param factory The component factory to register.
     * @param <C>     The component type constructed by the given {@code factory}.
     * @return The current instance of the {@code ComponentRegistry} for a fluent API.
     */
    <C> ComponentRegistry registerFactory(ComponentFactory<C> factory);

    /**
     * Sets the {@link OverridePolicy} for this {@code ComponentRegistry}.
     * <p>
     * This policy dictates what should happen when components are registered with an identifier for which another
     * component is already present.
     *
     * @param overridePolicy The override policy for components defined in this registry.
     * @return The current instance of the {@code Configurer} for a fluent API.
     */
    ComponentRegistry setOverridePolicy(OverridePolicy overridePolicy);
}
