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

// Added from update/pom.xml (source: F:\AxonFramework-main\AxonFramework-main\update\pom.xml).
// Skeleton only - dependencies wired and verified with a real build, no Java source converted yet
// (27 files under src/main/java; that's its own pass). Published (real org.axonframework:axon-update
// coordinate). Real leaf dependency-wise: only depends on axon-common, which is fully converted.
//
// See axonframework.java-conventions.gradle.kts for the canonical Maven scope/optional -> Gradle
// configuration mapping used throughout this file.

plugins {
    id("axonframework.published-conventions")
}

dependencies {
    api(project(":common"))
    testImplementation(project(mapOf("path" to ":common", "configuration" to "testArtifacts")))

    // Jakarta - default scope, not optional in the Maven source
    api("jakarta.annotation:jakarta.annotation-api:3.0.0")

    // Deliberate divergence from update/pom.xml, which doesn't declare this at all: common's own
    // compileOnly (Maven: provided) JSR-305 dependency is never transitive, even through an api project
    // dependency, but common's compiled bytecode is full of real javax.annotation.Nonnull/@Nullable
    // annotations. javac tolerates resolving update's source against that bytecode without this on the
    // classpath (it doesn't need to resolve an indirectly-referenced, class-retention annotation type),
    // but Eclipse's own compiler does not, and correctly reports "cannot resolve javax.annotation.Nonnull"
    // for any update file that touches an annotated common type. Confirmed this exact gap exists in the
    // real upstream Maven source too (common/pom.xml has the same provided-scope declaration, update/pom.xml
    // doesn't reference it either) - not introduced by this fork's conversion, just never surfaced there.
    // Kept compileOnly here too, matching common's own scope choice, so it doesn't leak into either
    // module's published artifact.
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "org.axonframework.update")
    }
}

// Deliberately not yet replicated from the Maven source:
// - maven-enforcer-plugin's enforce-banned-dependencies rule (bans non-test-scope org.springframework
//   dependencies). No Gradle built-in equivalent; nothing currently violates it. Same as common.
