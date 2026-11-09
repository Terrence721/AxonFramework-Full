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
 * See the License for the specific language governing permissions and limitations
 * under the License.
 */

package org.axonframework.messaging.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QualifiedNameTest {
    @Test
    void constructorSetsNamespaceAndLocalName() {
        QualifiedName name = new QualifiedName("org", "event");
        assertEquals("org", name.namespace());
        assertEquals("event", name.localName());
    }

    @Test
    void singleNameHasNoNamespace() {
        QualifiedName name = new QualifiedName("entity");

        assertNull(name.namespace());
        assertEquals("entity", name.localName());
        assertEquals("entity", name.toString());
    }

    @Test
    void createdFromAClassUsesItsPackageName() {
        QualifiedName name = new QualifiedName(String.class);

        assertEquals("java.lang", name.namespace());
        assertEquals("String", name.localName());
    }


    @Test
    void nullNameThrows() {
        assertThrows(NullPointerException.class, () -> new QualifiedName((String) null));
    }

    @Test
    void blankNameThrows() {
        assertThrows(IllegalArgumentException.class, () -> new QualifiedName("   "));
    }

    @Test
    void hashNameThrows() {
        assertThrows(IllegalArgumentException.class, () -> new QualifiedName("a#b"));
    }

    @Test
    void namespaceAndLocalNameNullLocalThrows() {
        assertThrows(NullPointerException.class, () -> new QualifiedName("ns", null));
    }

    @Test
    void namespaceAndLocalNameEmptyLocalThrows() {
        assertThrows(IllegalArgumentException.class, () -> new QualifiedName("ns", ""));
    }

    @Test
    void namespaceAndLocalNameNullNamespaceFallsBack() {
        assertEquals("local", new QualifiedName(null, "local").name());
    }

    @Test
    void namespaceAndLocalNameEmptyNamespaceFallsBack() {
        assertEquals("local", new QualifiedName("", "local").name());
    }

    @Test
    void fromClassAnonymousFallsBackToName() {
        Runnable anonymous = new Runnable() {
            @Override
            public void run() {
            }
        };
        QualifiedName name = new QualifiedName(anonymous.getClass());
        org.junit.jupiter.api.Assertions.assertNotNull(name.name());
    }

    @Test
    void fullName() {
        QualifiedName name = new QualifiedName("ns.local");
        assertEquals("ns.local", name.fullName());
    }

}
