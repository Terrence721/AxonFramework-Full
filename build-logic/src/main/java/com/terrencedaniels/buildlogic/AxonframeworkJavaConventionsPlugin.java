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

package com.terrencedaniels.buildlogic;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.quality.CheckstyleExtension;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.toolchain.JavaLanguageVersion;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Hand-written replacement for the former {@code axonframework.java-conventions.gradle.kts} precompiled script
 * plugin. Rewritten as a plain {@link Plugin} class (rather than a Kotlin-script-compiled one) specifically to
 * remove {@code build-logic}'s dependency on Gradle's {@code kotlin-dsl} convenience plugin, which transitively
 * pulls in a version of {@code org.jetbrains.kotlin:kotlin-gradle-plugin} vulnerable to CVE-2026-53914 (unsafe
 * deserialization in Kotlin's build-cache metadata handling) with no patched version currently bundled by any
 * Gradle release, stable or release-candidate. See {@code todo.md} for the full investigation.
 * <p>
 * Covers everything {@code axon-parent} applies to every module via Maven inheritance EXCEPT
 * publishing/signing/Central-portal wiring, which comes from root {@code pom.xml}'s plugins/profiles and belongs
 * in {@link AxonframeworkPublishedConventionsPlugin} instead.
 * <p>
 * Canonical Maven {@code <scope>}/{@code <optional>} -&gt; Gradle configuration mapping, used by every module's
 * own {@code build.gradle.kts} (kept here once rather than repeated per module, since this is the one plugin
 * every module actually applies):
 * <pre>
 *   default (compile) scope, not optional -&gt; api        (propagates to consumers, matches Maven)
 *   optional=true                          -&gt; implementation (used internally, not exposed - the
 *                                              standard real-world mapping for Maven "optional";
 *                                              not a perfect semantic match, but the practical one)
 *   provided scope                         -&gt; compileOnly
 *   test scope                             -&gt; testImplementation
 * </pre>
 */
public class AxonframeworkJavaConventionsPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply("java-library");
        project.getPluginManager().apply("checkstyle");

        // gradle.properties' group/version only apply to the root project by default - Gradle doesn't
        // propagate them to subprojects on its own, unlike Maven's parent-POM inheritance. Every module
        // applies this plugin, so this is the one place to close that gap for all of them at once.
        project.setGroup(project.getRootProject().getGroup());
        project.setVersion(project.getRootProject().getVersion());

        JavaPluginExtension javaExtension = project.getExtensions().getByType(JavaPluginExtension.class);
        // Diverges from upstream Axon's Java 21 baseline (maven.compiler.source/target=21,
        // enforced as a floor by maven-enforcer-plugin) - deliberate choice, not an oversight.
        // Raises the minimum JDK required to consume this fork's published artifacts from 21 to 25.
        javaExtension.getToolchain().getLanguageVersion().set(JavaLanguageVersion.of(25));

        project.getTasks().withType(JavaCompile.class).configureEach(task -> {
            task.getOptions().setEncoding("UTF-8");
            task.getOptions().setDeprecation(true);
            // Retains formal parameter names in compiled classes - Axon relies on this reflectively
            // for message handler parameter resolution (maven-compiler-plugin's <parameters>true).
            task.getOptions().getCompilerArgs().add("-parameters");
        });

        CheckstyleExtension checkstyle = project.getExtensions().getByType(CheckstyleExtension.class);
        checkstyle.setToolVersion("13.10.0");
        checkstyle.setConfigFile(project.getRootProject().file("build/checkstyle.xml"));
        // Maven's includeTestSourceDirectory=true has no separate switch here - Gradle's checkstyle
        // plugin already creates a check task per source set (checkstyleMain, checkstyleTest) by default.

        // Checkstyle 13.10.0 pulls in Maven's doxia reporting modules (used for its site-report output
        // format, which this build never invokes - we only run checkstyleMain/checkstyleTest) that in turn
        // resolve a real, Dependabot-flagged vulnerable transitive version: commons-lang3 3.8.1
        // (uncontrolled recursion, CVE fixed in 3.18.0). Forced to the patched version here since it's not
        // a dependency Checkstyle's actual linting logic exercises - confirmed via
        // `gradle :common:dependencies --configuration checkstyle`.
        project.getConfigurations().named("checkstyle").configure(configuration ->
                configuration.getResolutionStrategy().force("org.apache.commons:commons-lang3:3.18.0"));

        // plexus-utils 3.3.0-3.6.0 (directory-traversal CVE, fixed in 3.6.1) shows up transitively from two
        // unrelated places: checkstyle's doxia dependency above (3.3.0), and test-logging's published
        // log4j-core-test dependency's own maven-core -> maven-model chain (3.6.0 - one patch version short
        // of the fix). Since test-logging is api-exposed and every module here depends on it in test scope,
        // this needs forcing project-wide (every configuration), not just the checkstyle one.
        project.getConfigurations().all(configuration ->
                configuration.getResolutionStrategy().force("org.codehaus.plexus:plexus-utils:3.6.1"));

        // Mockito-as-Java-agent workaround (avoids dynamic self-attachment warnings on newer JDKs).
        // Resolves the mockito-core jar itself via a dedicated, non-transitive configuration rather than
        // reaching into the local Maven repository the way build/parent/pom.xml's argLine did.
        Configuration mockitoAgent = project.getConfigurations().create("mockitoAgent", configuration -> {
            configuration.setTransitive(false);
            configuration.setCanBeConsumed(false);
        });

        DependencyHandler dependencies = project.getDependencies();
        // Plain <dependency> (default/compile scope) in build/parent/pom.xml, so it propagates to
        // consumers of this module's public API - api, not implementation.
        dependencies.add("api", "org.jspecify:jspecify:1.0.1");

        dependencies.add("testImplementation", dependencies.platform("org.junit:junit-bom:6.1.3"));
        dependencies.add("testImplementation", "org.junit.jupiter:junit-jupiter");
        dependencies.add("testImplementation", "org.mockito:mockito-core:5.23.0");
        dependencies.add("testImplementation", "org.mockito:mockito-junit-jupiter:5.23.0");
        dependencies.add("testImplementation", "org.assertj:assertj-core:3.27.7");
        dependencies.add("testImplementation", "org.awaitility:awaitility:4.3.0");
        dependencies.add("testImplementation", "com.tngtech.archunit:archunit-junit5:1.5.0");
        // Guarded: test-logging/pom.xml is deliberately parented on the root aggregator rather than
        // build/parent for exactly this reason - unconditionally adding this here would make
        // test-logging depend on itself, since it applies this same convention plugin too.
        if (!project.getName().equals("test-logging")) {
            dependencies.add("testImplementation", dependencies.project(":test-logging"));
        }

        dependencies.add("mockitoAgent", "org.mockito:mockito-core:5.23.0");

        project.getTasks().named("test", Test.class).configure(test -> {
            test.useJUnitPlatform();
            test.systemProperty("java.awt.headless", "true");
            test.jvmArgs("-javaagent:" + mockitoAgent.getAsPath());

            // Passed as literal system properties in the source too (surefire's systemPropertyVariables),
            // apparently for tests that check SLF4J/Log4j behavior directly rather than dependency resolution.
            test.systemProperty("slf4j.version", "2.0.18");
            test.systemProperty("log4j.version", "2.26.1");

            // Maven's test.forkNumber=${surefire.forkNumber} has no direct Gradle equivalent (Gradle's own
            // per-worker identifier is org.gradle.test.worker, not a drop-in match) - not wired up until
            // we find the actual test code that reads this and can confirm what it needs.
        });

        // Maven's quick-install profile (skipTests=true): compile test classes (shared across modules
        // as dependencies) without running them. -PquickInstall on the command line for the same effect.
        if (project.getProviders().gradleProperty("quickInstall").isPresent()) {
            project.getTasks().named("test", Test.class).configure(test -> test.setEnabled(false));
        }

        project.getTasks().named("jar", Jar.class).configure(jar -> {
            // maven-jar-plugin's addDefaultImplementationEntries=true equivalent. Upstream derives
            // Implementation-Vendor from root pom.xml's <organization> (AxonIQ B.V.) - this fork
            // publishes under its own ownership, matching the POM metadata in published-conventions.
            Map<String, Object> attributes = new LinkedHashMap<>();
            attributes.put("Implementation-Title", project.getName());
            attributes.put("Implementation-Version", project.getVersion());
            attributes.put("Implementation-Vendor", "Terrence Daniels");
            jar.getManifest().attributes(attributes);
        });
    }
}
