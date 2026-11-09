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
 * See the License for the specific language governing permissions and limitations
 * under the License.
 */

package org.axonframework.common.property;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link PropertyAccessStrategy} registry and ordering, using the strategies registered
 * through the {@link java.util.ServiceLoader} by default.
 */
class PropertyAccessStrategyTest {

    private final List<PropertyAccessStrategy> registered = new ArrayList<>();

    @AfterEach
    void tearDown() {
        registered.forEach(PropertyAccessStrategy::unregister);
    }

    private void register(PropertyAccessStrategy strategy) {
        PropertyAccessStrategy.register(strategy);
        registered.add(strategy);
    }

    /**
     * Has a getter, a uniform accessor and a public field, all for the property {@code name}, each returning a
     * different value so the winning strategy can be told apart.
     */
    public static class AllAccessors {

        public String name = "field";

        public String getName() {
            return "bean";
        }

        public String name() {
            return "uniform";
        }
    }

    public static class UniformAndField {

        public String name = "field";

        public String name() {
            return "uniform";
        }
    }

    public static class FieldOnly {

        public String name = "field";
    }

    @Test
    void beanGetterWinsOverUniformAccessorAndField() {
        Property<AllAccessors> property = PropertyAccessStrategy.getProperty(AllAccessors.class, "name");

        assertNotNull(property);
        assertEquals("bean", property.getValue(new AllAccessors()));
    }

    @Test
    void uniformAccessorWinsOverField() {
        Property<UniformAndField> property = PropertyAccessStrategy.getProperty(UniformAndField.class, "name");

        assertNotNull(property);
        assertEquals("uniform", property.getValue(new UniformAndField()));
    }

    @Test
    void fieldIsUsedWhenNoAccessorMethodExists() {
        Property<FieldOnly> property = PropertyAccessStrategy.getProperty(FieldOnly.class, "name");

        assertNotNull(property);
        assertEquals("field", property.getValue(new FieldOnly()));
    }

    @Test
    void getPropertyReturnsNullWhenNoStrategyKnowsTheProperty() {
        assertNull(PropertyAccessStrategy.getProperty(FieldOnly.class, "unknown"));
    }

    @Test
    void registeredStrategyWithHigherPriorityIsConsultedFirst() {
        register(new FixedStrategy(100, "custom"));

        Property<FieldOnly> property = PropertyAccessStrategy.getProperty(FieldOnly.class, "name");

        assertNotNull(property);
        assertEquals("custom", property.getValue(new FieldOnly()));
    }

    @Test
    void strategyReturningNullFallsThroughToLowerPriorityStrategies() {
        register(new FixedStrategy(100, null));

        Property<FieldOnly> property = PropertyAccessStrategy.getProperty(FieldOnly.class, "name");

        assertNotNull(property);
        assertEquals("field", property.getValue(new FieldOnly()));
    }

    @Test
    void unregisteredStrategyIsNoLongerConsulted() {
        FixedStrategy strategy = new FixedStrategy(100, "custom");
        PropertyAccessStrategy.register(strategy);

        PropertyAccessStrategy.unregister(strategy);

        Property<FieldOnly> property = PropertyAccessStrategy.getProperty(FieldOnly.class, "name");
        assertNotNull(property);
        assertEquals("field", property.getValue(new FieldOnly()));
    }

    @Test
    void higherPrioritySortsBeforeLowerPriority() {
        PropertyAccessStrategy high = new FixedStrategy(10, "a");
        PropertyAccessStrategy low = new FixedStrategy(-10, "b");

        assertTrue(high.compareTo(low) < 0);
        assertTrue(low.compareTo(high) > 0);
        assertEquals(0, high.compareTo(high));
    }

    @Test
    void equalPriorityStrategiesOfDifferentClassesAreOrderedByClassName() {
        PropertyAccessStrategy first = new FixedStrategy(5, "a");
        PropertyAccessStrategy second = new OtherFixedStrategy(5, "b");

        assertNotEquals(0, first.compareTo(second));
        assertEquals(-Integer.signum(first.compareTo(second)), Integer.signum(second.compareTo(first)));
        assertNotEquals(first, second);
    }

    @Test
    void strategiesOfSameClassAndPriorityAreEqualAndShareAHashCode() {
        PropertyAccessStrategy first = new FixedStrategy(5, "a");
        PropertyAccessStrategy second = new FixedStrategy(5, "b");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void strategiesOfSameClassButDifferentPriorityAreNotEqual() {
        assertNotEquals(new FixedStrategy(5, "a"), new FixedStrategy(6, "a"));
    }

    @Test
    void strategyIsNotEqualToOtherTypes() {
        assertNotEquals(new FixedStrategy(5, "a"), "a string");
        assertNotEquals(null, new FixedStrategy(5, "a"));
    }

    private static class FixedStrategy extends PropertyAccessStrategy {

        private final int priority;
        private final @Nullable String value;

        private FixedStrategy(int priority, @Nullable String value) {
            this.priority = priority;
            this.value = value;
        }

        @Override
        protected int getPriority() {
            return priority;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected <T> @Nullable Property<T> propertyFor(Class<? extends T> targetClass, String property) {
            return value == null ? null : new Property<>() {
                @Override
                public <V> @Nullable V getValue(T target) {
                    return (V) value;
                }
            };
        }
    }

    private static class OtherFixedStrategy extends FixedStrategy {

        private OtherFixedStrategy(int priority, @Nullable String value) {
            super(priority, value);
        }
    }
}
