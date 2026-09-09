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

plugins {
    `kotlin-dsl`
}

// Deliberately NOT 25, unlike axonframework.java-conventions: build-logic only *specifies*
// JDK 25 for the real modules (common/update/test-logging) via that convention plugin - it never
// needs to run on JDK 25 itself, since it has no Java 25 language features to compile against and
// Kotlin doesn't even support emitting JVM 25 bytecode yet (falls back to JVM_24 regardless of
// what's requested). Previously pinned to 25 "to match," which had a real, then-unnoticed cost:
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

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}
