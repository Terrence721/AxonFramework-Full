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

// Added from test-logging/pom.xml (source: F:\AxonFramework-main\AxonFramework-main\test-logging\pom.xml).
// Published (real org.axonframework:axon-test-logging coordinate, not internal) - other modules
// depend on it in test scope for the shared Log4j2 config in src/main/resources.

plugins {
    id("axonframework.published-conventions")
}

dependencies {
    // Compile (default) scope in the Maven source, so that consumers who add this module as a
    // test dependency transitively receive these too - api, not implementation, same reasoning
    // as jspecify in axonframework.java-conventions.gradle.kts.
    api("org.slf4j:jul-to-slf4j:2.0.18")
    api("org.apache.logging.log4j:log4j-slf4j2-impl:2.26.1")
    api("org.slf4j:jcl-over-slf4j:2.0.18")
    api("org.apache.logging.log4j:log4j-core-test:2.26.1") {
        // Unused Spring Test integration; keeps this module's own dependency tree clean.
        exclude(group = "org.springframework", module = "spring-test")
    }
    api("org.apache.logging.log4j:log4j-core:2.26.1") {
        exclude(group = "com.sun.jdmk", module = "jmxtools")
        exclude(group = "com.sun.jmx", module = "jmxri")
        exclude(group = "javax.mail", module = "mail")
        exclude(group = "javax.jms", module = "jms")
    }
}
