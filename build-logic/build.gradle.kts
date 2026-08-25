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

// Matches the rest of the project's JDK 25 toolchain (axonframework.java-conventions). Kotlin
// doesn't support emitting JVM 25 bytecode yet and falls back to JVM_24 regardless of what's
// requested here - a cosmetic "Inconsistent JVM Target Compatibility" warning, not a build
// failure. The real consequence: build-logic's own compiled plugin classes require a JDK 25
// *runtime* just to load, not merely to compile, so any environment whose default JVM is older
// than 25 can't use this build - including GitHub's own automatic dependency-submission workflow
// for this repo (confirmed failing: "Dependency requires at least JVM runtime version 25. This
// build uses a Java 21 JVM."), worked around with our own explicit
// .github/workflows/dependency-submission.yml instead of changing this pin.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

kotlin {
    jvmToolchain(25)
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}
