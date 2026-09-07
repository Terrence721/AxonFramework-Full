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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Internal helper that stores {@link DecoratorDefinition.CompletedDecoratorDefinition} registrations and applies
 * them to a {@link Components} collection, for a {@link DefaultComponentRegistry}.
 * <p>
 * This class is not part of the public API. It exists purely to encapsulate decorator storage and application so
 * that {@link DefaultComponentRegistry} can delegate to it without duplicating logic.
 *
 * @since 5.1.0
 */
@Internal
public class DecoratorDefinitions {

    private final List<DecoratorDefinition.CompletedDecoratorDefinition<?, ?>> definitions = new ArrayList<>();

    /**
     * Registers the given {@code definition}.
     *
     * @param definition The decorator definition to register.
     */
    public void register(DecoratorDefinition.CompletedDecoratorDefinition<?, ?> definition) {
        definitions.add(definition);
    }

    /**
     * Applies all registered decorator definitions to the given {@code components}, in {@link
     * DecoratorDefinition.CompletedDecoratorDefinition#order() order}.
     *
     * @param components The components to apply the registered decorator definitions to.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void applyTo(Components components) {
        definitions.sort(Comparator.comparingInt(DecoratorDefinition.CompletedDecoratorDefinition::order));
        for (DecoratorDefinition.CompletedDecoratorDefinition decorator : definitions) {
            for (Component.Identifier id : components.identifiers()) {
                if (decorator.matches(id)) {
                    components.replace(id, decorator::decorate);
                }
            }
        }
    }

    /**
     * Returns all registered decorator definitions.
     *
     * @return All registered decorator definitions.
     */
    public List<DecoratorDefinition.CompletedDecoratorDefinition<?, ?>> definitions() {
        return List.copyOf(definitions);
    }
}
