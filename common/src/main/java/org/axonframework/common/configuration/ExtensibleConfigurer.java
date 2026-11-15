/*
 * Copyright (c) 2010-2026. Axon Framework
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

import java.util.function.Supplier;

/**
 * Contract for registering {@link ConfigurationExtension}s with a configuration.
 * <p>
 * This interface provides the methods to create and retrieve configuration extensions, which are used to
 * provide additional configuration data alongside the base configuration. Extensions are typically
 * registered during configuration setup and can be used to extend the capabilities of the configuration.
 *
 * @since 5.1.0
 */
public interface ExtensibleConfigurer {

    /**
     * Register a configuration extension with a factory function that will create the extension instance.
     * <p>
     * The extension is registered using the extension's class as the key in the registry. When the extension
     * is later retrieved via {@link ExtendedConfiguration#extension(Class)}, the factory function will be
     * invoked to create the extension instance.
     *
     * @param <E> the type of the configuration extension to register
     * @param extensionClass the class of the configuration extension to register
     * @param extensionFactory the factory function that creates the extension instance
     * @return this configurable for method chaining
     */
    <E extends ConfigurationExtension<?>> ExtensibleConfigurer extend(Class<E> extensionClass,
                                    Supplier<? extends E> extensionFactory);

    /**
     * Register a configuration extension using a pre-created instance.
     * <p>
     * This is a convenience method that creates a supplier returning the given extension instance.
     * The extension is registered using the extension's class as the key in the registry.
     *
     * @param <E> the type of the configuration extension to register
     * @param extension the pre-created configuration extension instance
     * @return this configurable for method chaining
     */
    default <E extends ConfigurationExtension<?>> ExtensibleConfigurer extend(E extension) {
        java.util.function.Supplier<E> factory = () -> extension;
        @SuppressWarnings("unchecked")
        Class<E> clazz = (Class<E>) extension.getClass();
        return extend(clazz, factory);
    }
}