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
// Same Maven <scope>/<optional> -> Gradle configuration mapping as common/build.gradle.kts:
//   default (compile) scope, not optional -> api
//   test scope                             -> testImplementation

plugins {
    id("axonframework.published-conventions")
}

dependencies {
    api(project(":common"))
    testImplementation(project(mapOf("path" to ":common", "configuration" to "testArtifacts")))

    // Jakarta - default scope, not optional in the Maven source
    api("jakarta.annotation:jakarta.annotation-api:3.0.0")
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "org.axonframework.update")
    }
}

// Deliberately not yet replicated from the Maven source:
// - maven-enforcer-plugin's enforce-banned-dependencies rule (bans non-test-scope org.springframework
//   dependencies). No Gradle built-in equivalent; nothing currently violates it. Same as common.
