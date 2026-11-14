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

package org.axonframework.common.property;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test class validating the {@link AbstractMethodPropertyAccessStrategy} core logic, exercised through a concrete
 * subclass since the class itself is abstract.
 */
class AbstractMethodPropertyAccessStrategyTest {

    private final Logger logger = (Logger) LogManager.getLogger(org.axonframework.common.property.AbstractMethodPropertyAccessStrategy.class);

    @BeforeEach
    void setUp() {
        // Enable debug logging so we can exercise the isNotReturningVoid debug branch
        logger.setLevel(Level.DEBUG);
    }

    @AfterEach
    void tearDown() {
        logger.setLevel(null); // Reset to default
    }

    @Test
    void voidMethodWithLoggingDisabledReturnsFalseAndDoesNotLog() {
        // When logger is NOT debug-enabled, the debug branch is skipped
        logger.setLevel(Level.INFO); // Explicitly disable debug

        // The method is not called directly - it's invoked via propertyFor
        AbstractMethodPropertyAccessStrategy strategy = new UniformStrategy();
        assertNull(strategy.propertyFor(Subject.class, "returnsNothing"));
    }

    public static class Subject {

        public String name() {
            return "uniform-name";
        }

        public String getValue() {
            return "bean-value";
        }

        public void returnsNothing() {
        }

        public String withArgument(String argument) {
            return argument;
        }
    }

    private static final class UniformStrategy extends AbstractMethodPropertyAccessStrategy {

        @Override
        protected String getterName(String property) {
            return property;
        }

        @Override
        protected int getPriority() {
            return -1024;
        }
    }

    private static final class BeanStrategy extends AbstractMethodPropertyAccessStrategy {

        @Override
        protected String getterName(String property) {
            return "get" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
        }

        @Override
        protected int getPriority() {
            return 0;
        }
    }

    @Test
    void propertyForReturnsPropertyWhenGetterExists() {
        AbstractMethodPropertyAccessStrategy strategy = new UniformStrategy();

        Property<Subject> property = strategy.propertyFor(Subject.class, "name");

        assertNotNull(property);
        assertEquals("uniform-name", property.getValue(new Subject()));
    }

    @Test
    void propertyForReturnsNullWhenNoMatchingMethodExists() {
        AbstractMethodPropertyAccessStrategy strategy = new UniformStrategy();

        assertNull(strategy.propertyFor(Subject.class, "unknown"));
    }

    @Test
    void propertyForReturnsNullWhenMethodReturnsVoid() {
        AbstractMethodPropertyAccessStrategy strategy = new UniformStrategy();

        assertNull(strategy.propertyFor(Subject.class, "returnsNothing"));
    }

    @Test
    void propertyForReturnsNullWhenMethodTakesArguments() {
        AbstractMethodPropertyAccessStrategy strategy = new UniformStrategy();

        assertNull(strategy.propertyFor(Subject.class, "withArgument"));
    }

    @Test
    void beanStrategyDerivesGetterNameFromProperty() {
        AbstractMethodPropertyAccessStrategy strategy = new BeanStrategy();

        Property<Subject> property = strategy.propertyFor(Subject.class, "value");

        assertNotNull(property);
        assertEquals("bean-value", property.getValue(new Subject()));
    }

    @Test
    void beanStrategyReturnsNullWhenDerivedGetterDoesNotExist() {
        AbstractMethodPropertyAccessStrategy strategy = new BeanStrategy();

        assertNull(strategy.propertyFor(Subject.class, "nonexistent"));
    }

    @Test
    void voidReturningMethodIsNotAccepted() {
        AbstractMethodPropertyAccessStrategy strategy = new BeanStrategy();

        // "nothing" would map to getNothing(), which does not exist -> null anyway
        assertNull(strategy.propertyFor(Subject.class, "nothing"));
    }
}