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

// Added from messaging/pom.xml (source: F:\AxonFramework-main\AxonFramework-main\messaging\pom.xml).
// Skeleton only - dependencies wired and verified with a real build, no Java source converted yet
// (534 main files under src/main/java; largest module in the reactor).
// Published (real org.axonframework:axon-messaging coordinate).

plugins {
    id("axonframework.published-conventions")
}

dependencies {
    // Axon
    api(project(":common"))
    api(project(":update"))
    api(project(":conversion"))

    // Jackson 2 (optional in Maven) - implementation, not api
    implementation(platform("com.fasterxml.jackson:jackson-bom:2.22.3"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.core:jackson-core")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Jackson 3 (optional in Maven)
    implementation(platform("tools.jackson:jackson-bom:3.2.2"))
    implementation("tools.jackson.core:jackson-databind")
    implementation("tools.jackson.core:jackson-core")

    // Caching (optional in Maven)
    implementation("javax.cache:cache-api:1.1.1")
    implementation("org.ehcache:ehcache:3.12.0")

    // Jakarta (optional in Maven)
    implementation("jakarta.validation:jakarta.validation-api:3.1.1")

    // Logging (default scope in Maven) - api so it propagates to consumers
    api("org.slf4j:slf4j-api:2.0.18")

    // Reactive
    implementation("io.projectreactor:reactor-core:3.8.6")
    api("org.reactivestreams:reactive-streams:1.0.4")

    // Persistence (optional in Maven)
    implementation("jakarta.persistence:jakarta.persistence-api:3.2.0")
    implementation("org.hibernate.orm:hibernate-core:7.4.5.Final")

    // Scheduling (optional in Maven)
    implementation("org.quartz-scheduler:quartz:2.5.2")
    implementation("org.jobrunr:jobrunr:8.8.1")
    implementation("com.github.kagkarlsson:db-scheduler:16.12.0")

    // Other (provided scope in Maven)
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
}
