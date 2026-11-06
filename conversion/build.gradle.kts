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

// Added from conversion/pom.xml (source: F:\AxonFramework-main\AxonFramework-main\conversion\pom.xml).
// Skeleton only - dependencies wired, no Java source converted yet (39 main files).
// Publishes a test-jar (deferred - see common/build.gradle.kts note for the same pattern).
// Published (real org.axonframework:axon-conversion coordinate).

plugins {
    id("axonframework.published-conventions")
}

dependencies {
    // Axon
    api(project(":common"))

    // Annotations - optional in Maven
    implementation("jakarta.annotation:jakarta.annotation-api:3.0.0")

    // Avro - optional in Maven
    implementation("org.apache.avro:avro:1.12.0")

    // Conversion - Jackson 2 (default scope in Maven) - api so it propagates to consumers.
    api(platform("com.fasterxml.jackson:jackson-bom:2.22.3"))
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.core:jackson-core")
    // optional in Maven -> implementation
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Conversion - Jackson 3 (default scope in Maven)
    api(platform("tools.jackson:jackson-bom:3.2.3"))
    api("tools.jackson.core:jackson-databind")
    api("tools.jackson.core:jackson-core")

    // Test - Jackson 2 extras (test scope)
    testImplementation("com.fasterxml.jackson.module:jackson-module-parameter-names")
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-cbor")

    // Test - Jackson 3 extras (test scope)
    testImplementation("tools.jackson.dataformat:jackson-dataformat-cbor")
}
