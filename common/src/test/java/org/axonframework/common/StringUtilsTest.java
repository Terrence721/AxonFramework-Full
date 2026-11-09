package org.axonframework.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringUtilsTest {

    @Test
    void nonEmptyOrNull() {
        assertTrue(StringUtils.nonEmptyOrNull("value"));
        assertFalse(StringUtils.nonEmptyOrNull(""));
        assertFalse(StringUtils.nonEmptyOrNull(null));
    }

    @Test
    void emptyOrNull() {
        assertTrue(StringUtils.emptyOrNull(null));
        assertTrue(StringUtils.emptyOrNull(""));
        assertFalse(StringUtils.emptyOrNull("value"));
    }

    @Test
    void nonEmpty() {
        assertTrue(StringUtils.nonEmpty("value"));
        assertFalse(StringUtils.nonEmpty(""));
    }

    @Test
    void lowerCaseFirstCharacter() {
        assertEquals("value", StringUtils.lowerCaseFirstCharacterOf("Value"));
        assertEquals("aBC", StringUtils.lowerCaseFirstCharacterOf("ABC"));
    }

    @Test
    void capitalize() {
        assertEquals("Value", StringUtils.capitalize("value"));
        assertEquals("", StringUtils.capitalize(""));
        assertNull(StringUtils.capitalize(null));
    }
}