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
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Internal helper that stores {@link ConfigurationEnhancer} registrations, tracks which ones are disabled or have
 * already been invoked, and discovers/invokes them, for a {@link DefaultComponentRegistry}.
 * <p>
 * This class is not part of the public API. It exists purely to encapsulate enhancer management so that
 * {@link DefaultComponentRegistry} can delegate to it without duplicating logic. Logging around registration and
 * disabling stays with the caller, so this class only reports outcomes ({@link DisableOutcome}) or previous
 * values, not messages.
 *
 * @since 5.1.0
 */
@Internal
public class ConfigurationEnhancers {

    private final Map<String, ConfigurationEnhancer> enhancers = new LinkedHashMap<>();
    private final List<Class<? extends ConfigurationEnhancer>> disabledEnhancers = new ArrayList<>();
    private final List<Class<? extends ConfigurationEnhancer>> invokedEnhancers = new ArrayList<>();
    private boolean scanningEnabled = true;

    /**
     * The outcome of a {@link #disable(Class)} call, so the caller can decide what (if anything) to log without
     * this class needing to know about logging.
     */
    public enum DisableOutcome {
        /**
         * The enhancer was not yet disabled and has now been disabled.
         */
        DISABLED,
        /**
         * The enhancer was already disabled; this call had no effect.
         */
        ALREADY_DISABLED,
        /**
         * The enhancer has already been invoked, so disabling it now has no effect.
         */
        TOO_LATE
    }

    /**
     * Registers the given {@code enhancer}.
     *
     * @param enhancer The enhancer to register.
     * @return The previously registered enhancer of the same concrete type, or {@code null} if there was none.
     */
    @Nullable
    public ConfigurationEnhancer register(ConfigurationEnhancer enhancer) {
        return enhancers.put(enhancer.getClass().getName(), enhancer);
    }

    /**
     * Returns all registered enhancers, keyed by their concrete class name.
     *
     * @return All registered enhancers, keyed by their concrete class name.
     */
    public Map<String, ConfigurationEnhancer> registered() {
        return Map.copyOf(enhancers);
    }

    /**
     * Disables the given enhancer {@code type}.
     *
     * @param type The type of enhancer to disable.
     * @return The outcome of this call.
     */
    public DisableOutcome disable(Class<? extends ConfigurationEnhancer> type) {
        if (invokedEnhancers.contains(type)) {
            return DisableOutcome.TOO_LATE;
        }
        if (disabledEnhancers.contains(type)) {
            return DisableOutcome.ALREADY_DISABLED;
        }
        disabledEnhancers.add(type);
        return DisableOutcome.DISABLED;
    }

    /**
     * Disables the given enhancer {@code types} unconditionally, without tracking outcomes. Intended for cloning
     * a set of already-disabled types into a new, not-yet-built registry - not for responding to a live
     * "disable this enhancer" request, which should use {@link #disable(Class)} instead.
     *
     * @param types The types of enhancer to disable.
     */
    public void disableAll(Collection<Class<? extends ConfigurationEnhancer>> types) {
        disabledEnhancers.addAll(types);
    }

    /**
     * Returns the types of all disabled enhancers.
     *
     * @return The types of all disabled enhancers.
     */
    public List<Class<? extends ConfigurationEnhancer>> disabledTypes() {
        return List.copyOf(disabledEnhancers);
    }

    /**
     * Discovers {@link ConfigurationEnhancer ConfigurationEnhancers} through the {@link ServiceLoader} mechanism,
     * using the given {@code classLoader}, excluding ones that are disabled or already registered.
     * <p>
     * Does not register the discovered enhancers itself - the caller is expected to do so (through
     * {@link #register(ConfigurationEnhancer)}), so that scanned and programmatically-registered enhancers are
     * treated identically by the caller.
     *
     * @param classLoader The class loader to discover enhancers with.
     * @return The discovered enhancers that are not disabled and not yet registered.
     */
    public List<ConfigurationEnhancer> discover(ClassLoader classLoader) {
        ServiceLoader<ConfigurationEnhancer> enhancerLoader = ServiceLoader.load(
                ConfigurationEnhancer.class, classLoader
        );
        return enhancerLoader.stream()
                              .map(provider -> provider.get())
                              .filter(enhancer -> !disabledEnhancers.contains(enhancer.getClass()))
                              .filter(this::isNotYetRegistered)
                              .toList();
    }

    private boolean isNotYetRegistered(ConfigurationEnhancer enhancer) {
        return !enhancers.containsKey(enhancer.getClass().getName());
    }

    /**
     * Invokes all registered, non-disabled enhancers on the given {@code registry}, in {@link
     * ConfigurationEnhancer#order()}.
     * <p>
     * Supports dynamic enhancer registration - if an enhancer registers another enhancer during its
     * {@link ConfigurationEnhancer#enhance(ComponentRegistry)} call, the newly registered enhancer is processed in
     * the correct order relative to all unprocessed enhancers. Each enhancer is processed one at a time to ensure
     * proper ordering when new enhancers are registered dynamically.
     *
     * @param registry The registry to hand to each enhancer's {@link ConfigurationEnhancer#enhance(ComponentRegistry)}.
     */
    public void invokeAll(ComponentRegistry registry) {
        Set<String> processedEnhancerKeys = new HashSet<>();

        while (processedEnhancerKeys.size() < enhancers.size()) {
            Optional<Map.Entry<String, ConfigurationEnhancer>> nextEnhancer =
                    enhancers.entrySet()
                             .stream()
                             .filter(entry -> !processedEnhancerKeys.contains(entry.getKey()))
                             .min(Comparator.comparingInt(entry -> entry.getValue().order()));

            if (nextEnhancer.isEmpty()) {
                break; // No more enhancers to process
            }

            Map.Entry<String, ConfigurationEnhancer> entry = nextEnhancer.get();
            String key = entry.getKey();
            ConfigurationEnhancer enhancer = entry.getValue();

            if (!disabledEnhancers.contains(enhancer.getClass())) {
                enhancer.enhance(registry);
                invokedEnhancers.add(enhancer.getClass());
            }
            processedEnhancerKeys.add(key);
        }
    }

    /**
     * Disables {@link #discover(ClassLoader) ServiceLoader-based} enhancer discovery.
     */
    public void disableScanning() {
        this.scanningEnabled = false;
    }

    /**
     * Checks whether {@link #discover(ClassLoader) ServiceLoader-based} enhancer discovery is enabled.
     *
     * @return {@code true} if enabled, {@code false} otherwise.
     */
    public boolean scanningEnabled() {
        return scanningEnabled;
    }
}
