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
    api(project(":modelling"))
    api(project(":messaging"))

    // Reactive (optional in Maven)
    implementation("io.projectreactor:reactor-core:3.8.6")
    api("org.reactivestreams:reactive-streams:1.0.4")

    // Persistence (optional in Maven)
    implementation("jakarta.persistence:jakarta.persistence-api:3.2.0")

    // Validation (optional in Maven)
    implementation("jakarta.validation:jakarta.validation-api:3.1.1")

    // Logging? Provided via messaging; ensure slf4j already transitive

    // Annotations - provided scope semantics
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
}
