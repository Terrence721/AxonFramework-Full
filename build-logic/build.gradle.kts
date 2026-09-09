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

// Was `kotlin-dsl` + three precompiled Kotlin-script convention plugins, until `kotlin-dsl` was found to be
// the sole source of a real, currently-unfixable Dependabot alert: it transitively pulls in
// org.jetbrains.kotlin:kotlin-gradle-plugin at whatever version Gradle's own release bundles (2.4.0 in Gradle
// 9.7.1, still 2.4.10 even in the 9.8.0 release candidate - verified by downloading and inspecting both
// distributions directly), vulnerable to CVE-2026-53914 (unsafe deserialization in Kotlin's build-cache
// metadata handling, fixed upstream in Kotlin 2.4.20, not yet bundled by any Gradle release). This project's
// build never enables Gradle's build cache anywhere, so the exploitable path was never actually reachable -
// but rather than just document that as an accepted gap, the three convention plugins were rewritten as plain
// Java classes (see src/main/java/com/terrencedaniels/buildlogic/) registered below, which removes Kotlin from
// build-logic entirely and eliminates the vulnerable dependency from this project's real dependency graph
// outright, not just to a newer version.
plugins {
    `java-gradle-plugin`
}

// Deliberately NOT 25, unlike axonframework.java-conventions: build-logic only *specifies*
// JDK 25 for the real modules (common/update/test-logging) via that convention plugin - it never
// needs to run on JDK 25 itself. Previously pinned to 25 "to match," which had a real, then-unnoticed cost:
// build-logic's own compiled plugin classes required a JDK 25 *runtime* just to load, breaking any
// environment whose default JVM is older - including GitHub's own automatic dependency-submission
// workflow for this repo (confirmed failing: "Dependency requires at least JVM runtime version 25.
// This build uses a Java 21 JVM."). Lowered to 21 (Gradle's own minimum-supported baseline) so that
// workflow - and any other JDK-21-default environment - can load this build's plugin classpath
// without needing its own JDK 25 setup step, while every real module's published artifacts still
// require JDK 25 exactly as before (that requirement lives entirely in
// axonframework.java-conventions.gradle.kts, untouched by this file).
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

// java-gradle-plugin doesn't auto-populate this registry the way kotlin-dsl's precompiled-script-plugin
// support used to (that mechanism derived a plugin ID from each .gradle.kts file's own name automatically) -
// now explicit, one entry per plugin class.
gradlePlugin {
    plugins {
        create("javaConventions") {
            id = "axonframework.java-conventions"
            implementationClass = "com.terrencedaniels.buildlogic.AxonframeworkJavaConventionsPlugin"
        }
        create("publishedConventions") {
            id = "axonframework.published-conventions"
            implementationClass = "com.terrencedaniels.buildlogic.AxonframeworkPublishedConventionsPlugin"
        }
        create("internalConventions") {
            id = "axonframework.internal-conventions"
            implementationClass = "com.terrencedaniels.buildlogic.AxonframeworkInternalConventionsPlugin"
        }
    }
}
