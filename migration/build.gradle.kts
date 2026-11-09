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
    implementation(platform("org.openrewrite.recipe:rewrite-recipe-bom:3.37.0"))

    implementation("org.openrewrite:rewrite-java")
    implementation("org.openrewrite:rewrite-kotlin")
    implementation("org.openrewrite:rewrite-maven")
    implementation("org.openrewrite.recipe:rewrite-java-dependencies")
    implementation("org.openrewrite:rewrite-yaml")
    implementation("org.openrewrite:rewrite-properties")
    implementation("org.openrewrite.recipe:rewrite-migrate-java")
    implementation("org.openrewrite.recipe:rewrite-spring")

    // OpenRewrite's rewrite-core pulls in micrometer-core 1.9.17, which is affected by CVE-2026-40984
    // (HTTP server instrumentation DoS; no 1.9.x fix exists). Align with the version used by
    // :metrics-micrometer, which is unaffected.
    constraints {
        implementation("io.micrometer:micrometer-core:1.17.2") {
            because("CVE-2026-40984: rewrite-core's transitive micrometer-core 1.9.17 is vulnerable")
        }
    }
}
