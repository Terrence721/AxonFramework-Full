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

// Added from build/parent/pom.xml (source: F:\AxonFramework-main\AxonFramework-main\build\parent\pom.xml).
// Covers everything axon-parent applies to every module via Maven inheritance EXCEPT
// publishing/signing/Central-portal wiring, which comes from root pom.xml's plugins/profiles
// and belongs in a separate convention plugin.

plugins {
    `java-library`
    checkstyle
}

// gradle.properties' group/version only apply to the root project by default - Gradle doesn't
// propagate them to subprojects on its own, unlike Maven's parent-POM inheritance. Every module
// applies this plugin, so this is the one place to close that gap for all of them at once.
group = rootProject.group
version = rootProject.version

java {
    toolchain {
        // Diverges from upstream Axon's Java 21 baseline (maven.compiler.source/target=21,
        // enforced as a floor by maven-enforcer-plugin) - deliberate choice, not an oversight.
        // Raises the minimum JDK required to consume this fork's published artifacts from 21 to 25.
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.isDeprecation = true
    // Retains formal parameter names in compiled classes - Axon relies on this reflectively
    // for message handler parameter resolution (maven-compiler-plugin's <parameters>true).
    options.compilerArgs.add("-parameters")
}

checkstyle {
    toolVersion = "13.10.0"
    configFile = rootProject.file("build/checkstyle.xml")
    // Maven's includeTestSourceDirectory=true has no separate switch here - Gradle's checkstyle
    // plugin already creates a check task per source set (checkstyleMain, checkstyleTest) by default.
}

// Mockito-as-Java-agent workaround (avoids dynamic self-attachment warnings on newer JDKs).
// Resolves the mockito-core jar itself via a dedicated, non-transitive configuration rather than
// reaching into the local Maven repository the way build/parent/pom.xml's argLine did.
val mockitoAgent: Configuration by configurations.creating {
    isTransitive = false
    isCanBeConsumed = false
}

dependencies {
    // Plain <dependency> (default/compile scope) in build/parent/pom.xml, so it propagates to
    // consumers of this module's public API - api, not implementation.
    api("org.jspecify:jspecify:1.0.1")

    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.23.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.23.0")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("org.awaitility:awaitility:4.3.0")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.5.0")
    testImplementation(project(":test-logging"))

    mockitoAgent("org.mockito:mockito-core:5.23.0")
}

tasks.test {
    useJUnitPlatform()
    systemProperty("java.awt.headless", "true")
    jvmArgs("-javaagent:${mockitoAgent.asPath}")

    // Passed as literal system properties in the source too (surefire's systemPropertyVariables),
    // apparently for tests that check SLF4J/Log4j behavior directly rather than dependency resolution.
    systemProperty("slf4j.version", "2.0.18")
    systemProperty("log4j.version", "2.26.1")

    // Maven's test.forkNumber=${surefire.forkNumber} has no direct Gradle equivalent (Gradle's own
    // per-worker identifier is org.gradle.test.worker, not a drop-in match) - not wired up until
    // we find the actual test code that reads this and can confirm what it needs.
}

// Maven's quick-install profile (skipTests=true): compile test classes (shared across modules
// as dependencies) without running them. -PquickInstall on the command line for the same effect.
if (providers.gradleProperty("quickInstall").isPresent) {
    tasks.test {
        enabled = false
    }
}

tasks.jar {
    // maven-jar-plugin's addDefaultImplementationEntries=true equivalent.
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "AxonIQ B.V."
        )
    }
}
