package org.axonframework.common;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberAccessibilityCallbackTest {

    @SuppressWarnings("unused")
    private static void hidden() {
    }

    @Test
    void runMakesMemberAccessible() throws NoSuchMethodException {
        Method method = MemberAccessibilityCallbackTest.class.getDeclaredMethod("hidden");

        assertNull(new MemberAccessibilityCallback(method).run());
        assertTrue(method.canAccess(null));
    }
}