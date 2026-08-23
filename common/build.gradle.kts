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

// Added from common/pom.xml (source: F:\AxonFramework-main\AxonFramework-main\common\pom.xml).
// Skeleton only - dependencies wired and verified with a real build, no Java source converted yet
// (218 files under src/main/java alone; that's its own multi-session pass, not a same-day add).
// Published (real org.axonframework:axon-common coordinate, referenced elsewhere as a test-jar
// dependency, e.g. test/pom.xml). Genuinely a leaf module - every dependency below is external,
// unlike test/pom.xml (depends on axon-eventsourcing, not yet converted - not actually a leaf
// despite todo.md's "leaf modules" grouping saying otherwise).
//
// Maven <scope>/<optional> -> Gradle configuration mapping used throughout:
//   default (compile) scope, not optional -> api        (propagates to consumers, matches Maven)
//   optional=true                          -> implementation (used internally, not exposed - the
//                                              standard real-world mapping for Maven "optional";
//                                              not a perfect semantic match, but the practical one)
//   provided scope                         -> compileOnly
//   test scope                             -> testImplementation

plugins {
    id("axonframework.published-conventions")
}

dependencies {
    // Caching - optional in Maven
    implementation("javax.cache:cache-api:1.1.1")
    implementation("org.ehcache:ehcache:3.12.0")

    // Jakarta - optional in Maven
    implementation("jakarta.annotation:jakarta.annotation-api:3.0.0")
    implementation("jakarta.validation:jakarta.validation-api:3.1.1")

    // Conversion - jackson-databind/jackson-core are default scope (not optional) in the Maven
    // source, so they propagate to consumers there too - api, not implementation. jsr310 is
    // optional -> implementation. Platform import matches how junit-bom is already handled in
    // axonframework.java-conventions.gradle.kts.
    api(platform("com.fasterxml.jackson:jackson-bom:2.22.1"))
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.core:jackson-core")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Logging - default scope, not optional
    api("org.slf4j:slf4j-api:2.0.18")

    // Reactive - reactor-core is optional -> implementation; reactive-streams is default scope -> api
    implementation("io.projectreactor:reactor-core:3.8.6")
    api("org.reactivestreams:reactive-streams:1.0.4")

    // Persistence - both optional in Maven
    implementation("jakarta.persistence:jakarta.persistence-api:3.2.0")
    implementation("org.hibernate.orm:hibernate-core:7.4.5.Final")

    // Other - provided scope in Maven
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    // Spring - test scope only in Maven ("used for testing only!" per the source's own comment)
    testImplementation(platform("org.springframework:spring-framework-bom:6.2.19"))
    testImplementation("org.springframework:spring-context-support")
    testImplementation("org.springframework:spring-orm")
    testImplementation("org.springframework:spring-test")
    testImplementation("org.springframework:spring-tx")
    testImplementation(platform("org.springframework.security:spring-security-bom:6.5.11"))
    testImplementation("org.springframework.security:spring-security-config")

    // Validation - test scope in Maven
    testImplementation("org.hibernate.validator:hibernate-validator:9.1.3.Final")

    // Testing other
    testImplementation("io.projectreactor:reactor-test:3.8.6")
    testImplementation("org.glassfish.expressly:expressly:6.0.0")
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "org.axonframework.common")
    }
}

// Deliberately not yet replicated from the Maven source:
// - maven-jar-plugin's test-jar goal (Maven publishes a separate axon-common test-jar; test/pom.xml
//   depends on it). Gradle needs java-test-fixtures or a manual test-jar task for this - deferred
//   until the test module actually exists to verify it against, not built speculatively now.
// - maven-enforcer-plugin's enforce-banned-dependencies rule (bans non-test-scope org.springframework
//   dependencies). No Gradle built-in equivalent; nothing currently violates it. Noted, not enforced.
