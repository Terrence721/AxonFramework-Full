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

plugins {
    id("axonframework.published-conventions")
}

dependencies {
    // Axon
    api(project(":messaging"))

    // Cache (optional in Maven)
    implementation("javax.cache:cache-api:1.1.1")
    implementation("org.ehcache:ehcache:3.12.0")

    // Conversion (optional in Maven) - Jackson 2
    implementation(platform("com.fasterxml.jackson:jackson-bom:2.22.3"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Persistence (optional in Maven)
    implementation("jakarta.persistence:jakarta.persistence-api:3.2.0")

    // Logging (default scope in Maven) - api so it propagates to consumers
    api("org.slf4j:slf4j-api:2.0.18")

    // Annotations - provided scope semantics
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
}
