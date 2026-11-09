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

package org.axonframework.update.detection;

import org.axonframework.update.api.Artifact;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class validating the {@link AxonVersionDetector}, using class loaders over temporary class path roots.
 * <p>
 * The detector only inspects class path roots whose path contains {@code org/axonframework}, {@code io/axoniq} or
 * {@code test-classes}, so the temporary roots are created below such a directory name.
 */
class AxonVersionDetectorTest {

    @TempDir
    Path temp;

    private ClassLoader originalContextClassLoader;
    private URLClassLoader testClassLoader;

    @BeforeEach
    void rememberContextClassLoader() {
        originalContextClassLoader = Thread.currentThread().getContextClassLoader();
    }

    @AfterEach
    void restoreContextClassLoader() throws IOException {
        Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        if (testClassLoader != null) {
            testClassLoader.close();
        }
    }

    private void useClassPath(Path... roots) throws IOException {
        URL[] urls = new URL[roots.length];
        for (int i = 0; i < roots.length; i++) {
            urls[i] = roots[i].toUri().toURL();
        }
        // A null parent keeps the real class path out, so only the temporary roots are inspected.
        testClassLoader = new URLClassLoader(urls, null);
        Thread.currentThread().setContextClassLoader(testClassLoader);
    }

    private Path directoryRoot(String name, String... pomProperties) throws IOException {
        Path root = Files.createDirectories(temp.resolve(name).resolve("test-classes"));
        for (int i = 0; i < pomProperties.length; i++) {
            Path artifactDirectory = Files.createDirectories(root.resolve("META-INF/maven/org.axonframework/module-" + i));
            Files.writeString(artifactDirectory.resolve("pom.properties"), pomProperties[i]);
        }
        if (pomProperties.length == 0) {
            Files.createDirectories(root.resolve("META-INF/maven"));
        }
        return root;
    }

    private static String pom(String groupId, String artifactId, String version) {
        return "groupId=" + groupId + "\nartifactId=" + artifactId + "\nversion=" + version + "\n";
    }

    @Test
    void detectsArtifactsFromPomPropertiesInDirectoryRoots() throws IOException {
        useClassPath(directoryRoot("one", pom("org.axonframework", "axon-messaging", "5.0.0"),
                                   pom("io.axoniq", "axoniq-thing", "1.2.3")));

        List<Artifact> result = AxonVersionDetector.safeDetectAxonModules();

        assertEquals(2, result.size());
        assertTrue(result.contains(new Artifact("org.axonframework", "axon-messaging", "5.0.0")));
        assertTrue(result.contains(new Artifact("io.axoniq", "axoniq-thing", "1.2.3")));
    }

    @Test
    void skipsIncompletePomPropertiesButKeepsTheValidOnes() throws IOException {
        useClassPath(directoryRoot("one", "groupId=org.axonframework\nartifactId=axon-broken\n",
                                   pom("org.axonframework", "axon-valid", "5.0.0")));

        List<Artifact> result = AxonVersionDetector.safeDetectAxonModules();

        assertEquals(List.of(new Artifact("org.axonframework", "axon-valid", "5.0.0")), result);
    }

    @Test
    void removesDuplicateArtifactsFoundOnMultipleRoots() throws IOException {
        String pom = pom("org.axonframework", "axon-common", "5.0.0");
        useClassPath(directoryRoot("one", pom), directoryRoot("two", pom));

        assertEquals(List.of(new Artifact("org.axonframework", "axon-common", "5.0.0")),
                     AxonVersionDetector.safeDetectAxonModules());
    }

    @Test
    void detectsArtifactsFromPomPropertiesInsideJars() throws IOException {
        Path jar = Files.createDirectories(temp.resolve("org").resolve("axonframework")).resolve("lib.jar");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
            out.putNextEntry(new JarEntry("META-INF/"));
            out.closeEntry();
            out.putNextEntry(new JarEntry("META-INF/maven/"));
            out.closeEntry();
            out.putNextEntry(new JarEntry("META-INF/maven/org.axonframework/axon-in-jar/pom.properties"));
            writeUtf8(out, pom("org.axonframework", "axon-in-jar", "5.1.0"));
            out.closeEntry();
        }
        useClassPath(jar);

        List<Artifact> result = AxonVersionDetector.safeDetectAxonModules();

        assertEquals(List.of(new Artifact("org.axonframework", "axon-in-jar", "5.1.0")), result);
    }

    @Test
    void returnsEmptyListWhenNoMavenMetadataIsOnTheClassPath() throws IOException {
        useClassPath(Files.createDirectories(temp.resolve("empty").resolve("test-classes")));

        assertTrue(AxonVersionDetector.safeDetectAxonModules().isEmpty());
    }

    @Test
    void returnsEmptyListWhenDetectionFails() {
        Thread.currentThread().setContextClassLoader(null);

        assertTrue(AxonVersionDetector.safeDetectAxonModules().isEmpty());
    }

    private static void writeUtf8(OutputStream out, String content) throws IOException {
        out.write(content.getBytes(StandardCharsets.UTF_8));
    }
}
