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

import org.axonframework.common.Assert;
import org.axonframework.common.TypeReference;
import org.axonframework.common.annotation.Internal;
import org.axonframework.common.annotation.RegistrationScope;
import org.axonframework.common.configuration.Component.Identifier;
import org.axonframework.common.infra.ComponentDescriptor;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.not;
import static org.axonframework.common.annotation.AnnotationUtils.isTypeAnnotatedWithHavingAttributeValue;

/**
 * Default implementation of the {@link ComponentRegistry} allowing for reuse of {@link Component},
 * {@link ComponentDecorator}, {@link ConfigurationEnhancer}, and {@link Module} registration for the
 * {@link ApplicationConfigurer} and {@link Module} implementations alike.
 *
 * @since 5.0.0
 */
public class DefaultComponentRegistry implements ComponentRegistry, ConfigurableComponentRegistry {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Components components = new Components();
    private OverridePolicy overridePolicy = OverridePolicy.WARN;
    private final DecoratorDefinitions decoratorDefinitions = new DecoratorDefinitions();
    private final ConfigurationEnhancers configurationEnhancers = new ConfigurationEnhancers();
    private final Modules modules = new Modules();
    private final List<ComponentFactory<?>> factories = new ArrayList<>();

    private final AtomicReference<@Nullable Configuration> parentConfig = new AtomicReference<>();
    private final AtomicReference<@Nullable Configuration> initializedConfiguration = new AtomicReference<>();

    /**
     * Creates a clone of this registry from existing registry. The clone will include the same enhancers, disabled
     * enhancers and decorator definitions as the original. The enhancerScanning flag is set to false.
     *
     * @return A clone of the original.
     */
    DefaultComponentRegistry copyWithDecoratorsAndEnhancers() {
        return create(
                this.decoratorDefinitions.definitions(),
                this.configurationEnhancers.registered().values(),
                this.configurationEnhancers.disabledTypes()
        );
    }

    /**
     * Creates a new registry. This will include the provided enhancers, disabled enhancers and decorator definitions
     * except those annotated with {@link RegistrationScope}. The enhancerScanning flag is set to false.
     *
     * @param decoratorDefinitions The list of decorator definitions to copy.
     * @param enhancers            The list of enhancers to copy.
     * @param disabledEnhancers    The list of disabled enhancer types to copy.
     * @return A new default component registry.
     */
    @Internal
    public static DefaultComponentRegistry create(
            Collection<DecoratorDefinition.CompletedDecoratorDefinition<?, ?>> decoratorDefinitions,
            Collection<ConfigurationEnhancer> enhancers,
            Collection<Class<? extends ConfigurationEnhancer>> disabledEnhancers) {
        var registry = new DefaultComponentRegistry().disableEnhancerScanning();
        var shouldRegisterForChildRegistry = not(
                isTypeAnnotatedWithHavingAttributeValue(
                        RegistrationScope.class,
                        "scope",
                        RegistrationScope.Scope.CURRENT
                )
        );
        enhancers.stream()
                 .filter(shouldRegisterForChildRegistry)
                 .forEach(registry.configurationEnhancers::register);
        registry.configurationEnhancers.disableAll(
                disabledEnhancers.stream()
                                 .filter(shouldRegisterForChildRegistry)
                                 .toList()
        );
        decoratorDefinitions.stream()
                            .filter(shouldRegisterForChildRegistry)
                            .collect(Collectors.toSet())
                            .forEach(registry.decoratorDefinitions::register);
        return registry;
    }


    @Override
    public <C> ComponentRegistry registerComponent(ComponentDefinition<? extends C> definition) {
        requireNonNull(definition, "The ComponentDefinition must not be null.");
        if (!(definition instanceof ComponentDefinition.ComponentCreator<? extends C> creator)) {
            // The compiler should avoid this from happening.
            throw new IllegalArgumentException("Unsupported component definition type: " + definition);
        }

        Component<? extends C> component = creator.createComponent();
        Identifier<? extends C> id = component.identifier();
        logger.debug("Registering component [{}] of type [{}].", id.name(), id.type());
        if (overridePolicy == OverridePolicy.REJECT && hasComponent(id.typeAsClass(), id.name())) {
            throw new ComponentOverrideException(id.typeAsClass(), id.name());
        }

        Component<? extends C> previous = components.put(component);
        if (previous != null && overridePolicy == OverridePolicy.WARN) {
            logger.warn("Replaced a previous Component registered for type [{}] and name [{}].",
                        id.name(),
                        id.type());
        }
        return this;
    }

    @Override
    public <C> ComponentRegistry registerDecorator(DecoratorDefinition<C, ? extends C> definition) {
        requireNonNull(definition, "The decorator definition must not be null.");
        if (!(definition instanceof DecoratorDefinition.CompletedDecoratorDefinition<C, ? extends C> decoratorRegistration)) {
            // The compiler should avoid this from happening.
            throw new IllegalArgumentException("Unsupported decorator definition type: " + definition);
        }

        logger.debug("Registering decorator definition: [{}]", definition);
        decoratorDefinitions.register(decoratorRegistration);
        return this;
    }

    @Override
    public boolean hasComponent(Class<?> type,
                                @Nullable String name,
                                SearchScope searchScope) {
        return switch (searchScope) {
            case ALL -> components.contains(new Identifier<>(type, name)) || parentHasComponent(type, name);
            case CURRENT -> components.contains(new Identifier<>(type, name));
            case ANCESTORS -> parentHasComponent(type, name);
        };
    }

    private Boolean parentHasComponent(Class<?> type, @Nullable String name) {
        return Optional.ofNullable(parentConfig.get())
                       .map(parent -> parent.hasComponent(type, name)).orElse(false);
    }

    @Override
    public ComponentRegistry registerEnhancer(ConfigurationEnhancer enhancer) {
        logger.debug("Registering enhancer [{}].", enhancer.getClass().getSimpleName());
        ConfigurationEnhancer previous = configurationEnhancers.register(enhancer);
        if (previous != null) {
            logger.warn("Duplicate Configuration Enhancer registration dedicated. Replaced enhancer of type [{}].",
                        enhancer.getClass().getSimpleName());
        }
        return this;
    }

    @Override
    public ComponentRegistry registerModule(Module module) {
        if (logger.isDebugEnabled()) {
            logger.debug("Registering module [{}].", module.name());
        }
        modules.register(module);
        return this;
    }

    @Override
    public <C> ComponentRegistry registerFactory(ComponentFactory<C> factory) {
        if (logger.isDebugEnabled()) {
            logger.debug("Registering component factory [{}].", factory.getClass().getSimpleName());
        }
        this.factories.add(factory);
        return this;
    }

    /**
     * Builds the {@link Configuration} from this {@code ComponentRegistry} as a root configuration.
     * <p>
     * The given {@code lifecycleRegistry} is used to register components' lifecycle methods.
     *
     * @param lifecycleRegistry The registry where lifecycle handlers are registered.
     * @return A fully initialized configuration exposing all configured components.
     */
    public Configuration build(LifecycleRegistry lifecycleRegistry) {
        return doBuild(null, lifecycleRegistry);
    }

    /**
     * Builds the {@link Configuration} from this {@code ComponentRegistry} as a nested configuration under the given
     * {@code parent}.
     * <p>
     * Components registered in the {@code parent} are available to components registered in this registry, but not vice
     * versa. The given {@code lifecycleRegistry} is used to register components' lifecycle methods.
     *
     * @param parent            The parent configuration.
     * @param lifecycleRegistry The registry where lifecycle handlers are registered.
     * @return A fully initialized configuration exposing all configured components.
     */
    public Configuration buildNested(Configuration parent,
                                     LifecycleRegistry lifecycleRegistry) {
        return doBuild(requireNonNull(parent), requireNonNull(lifecycleRegistry));
    }

    private Configuration doBuild(@Nullable Configuration optionalParent,
                                  LifecycleRegistry lifecycleRegistry) {
        Configuration configuration = initializedConfiguration.get();
        if (configuration != null) {
            return configuration;
        }
        this.parentConfig.set(optionalParent);
        if (configurationEnhancers.scanningEnabled()) {
            configurationEnhancers.discover(getClass().getClassLoader()).forEach(this::registerEnhancer);
        }
        configurationEnhancers.invokeAll(this);
        decoratorDefinitions.applyTo(components);
        Configuration currentConfiguration = createLocalConfiguration(this.parentConfig.get());

        buildModules(currentConfiguration, lifecycleRegistry);
        initializeComponents(currentConfiguration, lifecycleRegistry);
        registerFactoryShutdownHandlers(lifecycleRegistry);
        initializedConfiguration.set(currentConfiguration);

        return currentConfiguration;
    }

    /**
     * Creates a local configuration for a given parent, backed by this registry's components.
     * <p>
     * This method is idempotent with respect to the owning registry: if the given {@code parent} is already a
     * {@link LocalConfiguration} belonging to this registry, it is returned as-is rather than being wrapped in an
     * additional layer. This guarantees that at most one {@code LocalConfiguration} per registry exists in any
     * parent chain, so that lazy component definitions stored in the shared {@link Components} are always resolved
     * through a single entry point.
     *
     * @param parent The optional parent configuration to serve as parent for the created result
     * @return A configuration backed by this registry. Either a new {@link LocalConfiguration} wrapping the given
     * {@code parent}, or the {@code parent} itself if it already belongs to this registry.
     */
    @Internal
    public Configuration createLocalConfiguration(@Nullable Configuration parent) {
        Configuration currentConfiguration =
                parent instanceof LocalConfiguration lc && lc.enclosingRegistry() == this
                        ? parent
                        : new LocalConfiguration(parent);
        if (!this.hasComponent(ComponentRegistry.class, SearchScope.CURRENT)) {
            registerComponent(ComponentDefinition.ofType(ComponentRegistry.class)
                                                 .withInstance(this)); // register itself
        }
        return currentConfiguration;
    }

    /**
     * Ensure all registered {@link Module Modules} are built too. Store their {@link Configuration} results for
     * exposure on {@link Configuration#getModuleConfigurations()}.
     */
    private void buildModules(Configuration configuration, LifecycleRegistry lifecycleRegistry) {
        for (Module module : modules.registered()) {
            var moduleRegistry = this.copyWithDecoratorsAndEnhancers();
            var builtModuleConfiguration = HierarchicalLifecycleRegistry.build(
                    lifecycleRegistry,
                    childLifecycleRegistry -> {
                        var local = moduleRegistry.createLocalConfiguration(configuration);
                        var moduleConfiguration = module.build(local, childLifecycleRegistry);
                        return moduleRegistry.buildNested(moduleConfiguration, childLifecycleRegistry);
                    }
            );
            modules.recordBuildResult(module.name(), builtModuleConfiguration);
        }
    }

    /**
     * Initialize the components defined in this registry, allowing them to register their lifecycle actions with given
     * {@code lifecycleRegistry}.
     *
     * @param configuration     The current configuration to apply.
     * @param lifecycleRegistry The registry where components may register their lifecycle actions.
     */
    private void initializeComponents(Configuration configuration, LifecycleRegistry lifecycleRegistry) {
        components.postProcessComponents(c -> c.initLifecycle(configuration, lifecycleRegistry));
    }

    /**
     * Registers the shutdown handlers for all
     * {@link #registerFactory(ComponentFactory) registered ComponentFactories}.
     *
     * @param lifecycleRegistry The registry where {@link ComponentFactory ComponentFactories} may register their
     *                          shutdown operations.
     */
    private void registerFactoryShutdownHandlers(LifecycleRegistry lifecycleRegistry) {
        factories.forEach(factory -> factory.registerShutdownHandlers(lifecycleRegistry));
    }

    @Override
    public DefaultComponentRegistry setOverridePolicy(OverridePolicy overridePolicy) {
        this.overridePolicy = requireNonNull(overridePolicy, "The override policy must not be null.");
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ComponentRegistry disableEnhancer(String fullyQualifiedClassName) {
        Objects.requireNonNull(fullyQualifiedClassName, "The fully qualified class name must not be null.");
        try {
            var enhancerClass = Class.forName(fullyQualifiedClassName);
            if (!ConfigurationEnhancer.class.isAssignableFrom(enhancerClass)) {
                throw new IllegalArgumentException(
                        String.format("Class %s is not a ConfigurationEnhancer", fullyQualifiedClassName)
                );
            }
            return disableEnhancer((Class<? extends ConfigurationEnhancer>) enhancerClass);
        } catch (ClassNotFoundException e) {
            logger.warn(
                    "Disabling Configuration Enhancer [{}] won't take effect as the enhancer class could not be found.",
                    fullyQualifiedClassName);
        }
        return this;
    }

    @Override
    public DefaultComponentRegistry disableEnhancer(Class<? extends ConfigurationEnhancer> enhancerClass) {
        switch (configurationEnhancers.disable(enhancerClass)) {
            case TOO_LATE -> logger.warn(
                    "Disabling Configuration Enhancer [{}] won't take effect as it has already been invoked. "
                            + "We recommend to invoke disabling of this enhancer before it takes effect.",
                    enhancerClass.getSimpleName());
            case DISABLED -> {
                if (logger.isInfoEnabled()) {
                    logger.info(
                            "Configuration Enhancer [{}] has been disabled. "
                                    + "Ensure components set by this enhancer are not mandatory in this application.",
                            enhancerClass
                    );
                }
            }
            case ALREADY_DISABLED -> {
                // No-op - matches the original silent no-op when an enhancer is already disabled.
            }
        }
        return this;
    }

    @Override
    public DefaultComponentRegistry disableEnhancerScanning() {
        configurationEnhancers.disableScanning();
        return this;
    }

    @Override
    public void describeTo(ComponentDescriptor descriptor) {
        descriptor.describeProperty("initialized", initializedConfiguration.get() != null);
        descriptor.describeProperty("components", components);
        descriptor.describeProperty("decorators", decoratorDefinitions.definitions());
        descriptor.describeProperty("configurerEnhancers", configurationEnhancers.registered());
        descriptor.describeProperty("modules", modules.registered());
        descriptor.describeProperty("factories", factories);
    }

    private class LocalConfiguration implements Configuration {

        @Nullable
        private final Configuration parent;

        /**
         * Construct a {@code LocalConfiguration} using the given {@code parent} configuration.
         * <p>
         * If this configuration does not have a certain {@link Component}, it will fall back to it's {@code parent}.
         * <p>
         * Note that the {@code parent} can be {@code null}.
         *
         * @param parent The parent life cycle supporting configuration to fall back on when necessary.
         */
        public LocalConfiguration(@Nullable Configuration parent) {
            this.parent = parent;
        }

        @Override
        public @Nullable Configuration getParent() {
            return parent;
        }

        /**
         * Returns the {@link DefaultComponentRegistry} that created this {@code LocalConfiguration}.
         *
         * @return The enclosing registry instance.
         */
        DefaultComponentRegistry enclosingRegistry() {
            return DefaultComponentRegistry.this;
        }

        @Override
        public <C> Optional<C> getOptionalComponent(Class<C> type,
                                                    @Nullable String name) {
            return components.get(new Identifier<>(type, name))
                             .map(c -> c.resolve(this))
                             .or(() -> {
                                 Optional<Component<C>> factoryComponent = fromFactory(type, name);
                                 if (factoryComponent.isPresent()) {
                                     components.put(factoryComponent.get());
                                     return factoryComponent.map(creator -> creator.resolve(this));
                                 }
                                 return Optional.empty();
                             })
                             .or(() -> Optional.ofNullable(fromParent(type, name, () -> null)));
        }

        @Override
        public <C> Optional<C> getOptionalComponent(TypeReference<C> typeReference,
                                                    @Nullable String name) {
            return components.getByTypeReference(new Identifier<>(typeReference, name))
                             .map(c -> c.resolve(this))
                             .or(() -> Optional.ofNullable(fromParent(typeReference, name, () -> null)));
        }

        @Override
        public <C> C getComponent(Class<C> type,
                                  @Nullable String name,
                                  Supplier<C> defaultImpl) {
            Identifier<C> identifier = new Identifier<>(type, name);
            Object component = components.computeIfAbsent(
                                                 identifier,
                                                 () -> fromFactory(type, name).orElseGet(
                                                         () -> new LazyInitializedComponentDefinition<>(
                                                                 identifier,
                                                                 c -> fromParent(type, name, defaultImpl)
                                                         )
                                                 )
                                         )
                                         .resolve(this);
            return type.cast(component);
        }

        @SuppressWarnings("unchecked")
        private <C> Optional<Component<C>> fromFactory(Class<C> type, @Nullable String name) {
            if (name == null) {
                // The ComponentFactory requires a non-null name at all times.
                return Optional.empty();
            }

            for (ComponentFactory<?> factory : factories) {
                if (!type.isAssignableFrom(factory.forType())) {
                    continue;
                }
                Optional<Component<C>> factoryComponent = ((ComponentFactory<C>) factory).construct(name, this);
                if (factoryComponent.isPresent()) {
                    return factoryComponent;
                }
            }
            return Optional.empty();
        }

        private <C> C fromParent(Class<C> type, @Nullable String name, Supplier<C> defaultSupplier) {
            return parent != null
                    ? parent.getOptionalComponent(type, name).orElseGet(defaultSupplier)
                    : defaultSupplier.get();
        }

        private <C> C fromParent(TypeReference<C> typeReference, @Nullable String name, Supplier<C> defaultSupplier) {
            return parent != null
                    ? parent.getOptionalComponent(typeReference, name).orElseGet(defaultSupplier)
                    : defaultSupplier.get();
        }


        @Override
        public List<Configuration> getModuleConfigurations() {
            return modules.builtConfigurations();
        }

        @Override
        public void describeTo(ComponentDescriptor descriptor) {
            descriptor.describeProperty("components", components);
            descriptor.describeProperty("modules", modules.builtConfigurations());
        }


        @Override
        public Optional<Configuration> getModuleConfiguration(String name) {
            Assert.nonEmpty(name, "The name must not be empty or null.");
            return modules.builtConfiguration(name);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <C> Map<String, C> getComponents(Class<C> type) {
            Map<String, C> result = new LinkedHashMap<>();

            // 1. Collect from current configuration's components
            components.identifiers().stream()
                      .filter(identifier -> type.isAssignableFrom(identifier.typeAsClass()))
                      .map(identifier -> (Identifier<C>) identifier)
                      .forEach(identifier -> {
                          components.get(identifier)
                                    .ifPresent(component -> result.put(identifier.name(), component.resolve(this)));
                      });
            // 2. Collect from all module configurations (recursively)
            for (Configuration moduleConfig : getModuleConfigurations()) {
                Map<String, C> moduleComponents = moduleConfig.getComponents(type);
                // Note: module components might override main components with same name
                result.putAll(moduleComponents);
            }

            return Collections.unmodifiableMap(result);
        }
    }
}
