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

package org.axonframework.common.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link MavenArtifactVersionResolver}.
 */
class MavenArtifactVersionResolverTest {

    private static final String PROPERTIES_PATH = "META-INF/maven/org.example/example-artifact/pom.properties";

    /**
     * A {@link ClassLoader} serving a single resource from memory.
     */
    private static ClassLoader classLoaderServing(String path, InputStream stream) {
        return new ClassLoader(null) {
            @Override
            public InputStream getResourceAsStream(String name) {
                return path.equals(name) ? stream : null;
            }
        };
    }

    private static InputStream propertiesOf(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.ISO_8859_1));
    }

    @Test
    void getReturnsVersionFromPomProperties() throws IOException {
        ClassLoader classLoader = classLoaderServing(
                PROPERTIES_PATH, propertiesOf("groupId=org.example\nartifactId=example-artifact\nversion=1.2.3\n")
        );
        MavenArtifactVersionResolver testSubject =
                new MavenArtifactVersionResolver("org.example", "example-artifact", classLoader);

        assertEquals("1.2.3", testSubject.get());
    }

    @Test
    void getReturnsNullWhenPomPropertiesIsAbsent() throws IOException {
        MavenArtifactVersionResolver testSubject = new MavenArtifactVersionResolver(
                "org.example", "unknown-artifact", classLoaderServing(PROPERTIES_PATH, propertiesOf("version=1"))
        );

        assertNull(testSubject.get());
    }

    @Test
    void getReturnsNullWhenPomPropertiesHasNoVersion() throws IOException {
        ClassLoader classLoader = classLoaderServing(PROPERTIES_PATH, propertiesOf("artifactId=example-artifact\n"));
        MavenArtifactVersionResolver testSubject =
                new MavenArtifactVersionResolver("org.example", "example-artifact", classLoader);

        assertNull(testSubject.get());
    }

    @Test
    void getClosesTheResourceStream() throws IOException {
        AtomicBoolean closed = new AtomicBoolean();
        InputStream stream = new ByteArrayInputStream("version=9.9\n".getBytes(StandardCharsets.ISO_8859_1)) {
            @Override
            public void close() throws IOException {
                closed.set(true);
                super.close();
            }
        };
        MavenArtifactVersionResolver testSubject = new MavenArtifactVersionResolver(
                "org.example", "example-artifact", classLoaderServing(PROPERTIES_PATH, stream)
        );

        assertEquals("9.9", testSubject.get());
        assertTrue(closed.get());
    }

    @Test
    void getPropagatesIoExceptionAndStillClosesTheStream() {
        AtomicBoolean closed = new AtomicBoolean();
        InputStream failing = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("unreadable");
            }

            @Override
            public void close() {
                closed.set(true);
            }
        };
        MavenArtifactVersionResolver testSubject = new MavenArtifactVersionResolver(
                "org.example", "example-artifact", classLoaderServing(PROPERTIES_PATH, failing)
        );

        IOException exception = assertThrows(IOException.class, testSubject::get);

        assertEquals("unreadable", exception.getMessage());
        assertTrue(closed.get());
    }
}
