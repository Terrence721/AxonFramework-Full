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

package org.axonframework.conversion;

import org.axonframework.common.infra.ComponentDescriptor;
import org.axonframework.common.infra.FilesystemStyleComponentDescriptor;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link DelegatingGeneralConverter} and the default {@link Converter#convert(Object, Class)}
 * method.
 */
class DelegatingGeneralConverterTest {

    private final RecordingConverter delegate = new RecordingConverter();

    @Test
    void constructorRejectsNullDelegate() {
        NullPointerException exception =
                assertThrows(NullPointerException.class, () -> new DelegatingGeneralConverter(null));

        assertEquals("The Converter must not be null.", exception.getMessage());
    }

    @Test
    void isAGeneralConverter() {
        assertInstanceOf(GeneralConverter.class, new DelegatingGeneralConverter(delegate));
    }

    @Test
    void convertDelegatesInputAndTargetTypeAndReturnsTheDelegateResult() {
        delegate.result = "converted";
        DelegatingGeneralConverter testSubject = new DelegatingGeneralConverter(delegate);

        String result = testSubject.convert("input", (Type) String.class);

        assertEquals("converted", result);
        assertEquals(List.of("input->" + String.class.getTypeName()), delegate.calls);
    }

    @Test
    void convertByClassGoesThroughTheTypeBasedMethodOfTheDelegate() {
        delegate.result = 42;
        DelegatingGeneralConverter testSubject = new DelegatingGeneralConverter(delegate);

        Integer result = testSubject.convert("41", Integer.class);

        assertEquals(42, result);
        assertEquals(List.of("41->java.lang.Integer"), delegate.calls);
    }

    @Test
    void nullInputAndNullResultAreDelegatedAsIs() {
        DelegatingGeneralConverter testSubject = new DelegatingGeneralConverter(delegate);

        assertNull(testSubject.convert(null, String.class));
        assertEquals(List.of("null->java.lang.String"), delegate.calls);
    }

    @Test
    void failuresOfTheDelegateAreNotWrapped() {
        IllegalStateException failure = new IllegalStateException("cannot convert");
        delegate.failure = failure;
        DelegatingGeneralConverter testSubject = new DelegatingGeneralConverter(delegate);

        IllegalStateException thrown =
                assertThrows(IllegalStateException.class, () -> testSubject.convert("x", String.class));

        assertSame(failure, thrown);
    }

    @Test
    void exposesTheWrappedConverter() {
        assertSame(delegate, new DelegatingGeneralConverter(delegate).delegate());
    }

    @Test
    void describesItselfAsAWrapperOfTheDelegate() {
        FilesystemStyleComponentDescriptor descriptor = new FilesystemStyleComponentDescriptor();

        new DelegatingGeneralConverter(delegate).describeTo(descriptor);

        assertTrue(descriptor.describe().contains("delegate-marker"), descriptor.describe());
    }

    @Test
    void passThroughConverterDoesNotChangeTheTypeBasedContract() {
        // The default Class based method of the interface must behave the same for any implementation
        assertEquals("same", PassThroughConverter.INSTANCE.convert("same", String.class));
        assertNull(PassThroughConverter.INSTANCE.convert(null, String.class));
    }

    private static final class RecordingConverter implements Converter {

        private final List<String> calls = new ArrayList<>();
        private Object result;
        private RuntimeException failure;

        @SuppressWarnings("unchecked")
        @Override
        public <T> @Nullable T convert(@Nullable Object input, Type targetType) {
            calls.add(input + "->" + targetType.getTypeName());
            if (failure != null) {
                throw failure;
            }
            return (T) result;
        }

        @Override
        public void describeTo(ComponentDescriptor descriptor) {
            descriptor.describeProperty("marker", "delegate-marker");
        }
    }
}
