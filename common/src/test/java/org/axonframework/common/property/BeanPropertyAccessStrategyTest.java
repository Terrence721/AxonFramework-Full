package org.axonframework.common.property;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class BeanPropertyAccessStrategyTest {

    static class Bean {
        private String value = "v";

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public void nothing() {
        }
    }

    @Test
    void findsGetterProperty() {
        BeanPropertyAccessStrategy strategy = new BeanPropertyAccessStrategy();

        Property<?> property = strategy.propertyFor(Bean.class, "value");

        assertNotNull(property);
    }

    @Test
    void returnsNullWhenNoGetter() {
        BeanPropertyAccessStrategy strategy = new BeanPropertyAccessStrategy();

        assertNull(strategy.propertyFor(Bean.class, "missing"));
    }

    @Test
    void voidReturningMethodNotAccepted() {
        BeanPropertyAccessStrategy strategy = new BeanPropertyAccessStrategy();

        assertNull(strategy.propertyFor(Bean.class, "nothing"));
    }
}