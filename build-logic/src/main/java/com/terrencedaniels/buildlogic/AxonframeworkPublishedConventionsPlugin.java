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
import org.gradle.api.plugins.BasePluginExtension;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.external.javadoc.StandardJavadocDocletOptions;
import org.gradle.plugins.signing.SigningExtension;

/**
 * Hand-written replacement for the former {@code axonframework.published-conventions.gradle.kts} precompiled
 * script plugin - see {@link AxonframeworkJavaConventionsPlugin} for why this is now a plain {@link Plugin}
 * class rather than a Kotlin script.
 * <p>
 * For modules that publish to Maven Central, layering on top of {@link AxonframeworkJavaConventionsPlugin}.
 * Covers what root {@code pom.xml}'s {@code central-publishing-maven-plugin}, its "javadoc"/"sources" profiles,
 * and its "sign" profile ({@code maven-gpg-plugin}) apply to every published module - all three of those
 * profiles only activate during an actual upstream release build ({@code maven-release-plugin}'s
 * {@code releaseProfiles=javadoc,sources,sign}), which is why signing below is gated on credentials being
 * present rather than always running. Central Portal upload itself is wired in the root {@code settings.gradle.kts}
 * (the {@code com.gradleup.nmcp.settings} plugin auto-applies {@code com.gradleup.nmcp} here - no plugin needed
 * in this class for that part).
 * <p>
 * POM metadata deliberately does NOT carry over root {@code pom.xml}'s AxonIQ organization, upstream
 * GitHub SCM URLs, or its named AxonIQ developers/emails - this fork publishes under its own maintainer and
 * repository, not upstream's identity.
 */
public class AxonframeworkPublishedConventionsPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(AxonframeworkJavaConventionsPlugin.class);
        project.getPluginManager().apply("maven-publish");
        project.getPluginManager().apply("signing");

        // Every Axon module's real Maven artifactId is "axon-<name>" (axon-common, axon-test-logging, ...),
        // but Gradle's base.archivesName defaults to the project directory name ("common", "test-logging"),
        // with no automatic "axon-" prefix. Centralized here rather than repeated per module, since every
        // published module needs it and it's easy to silently forget on any one of them.
        BasePluginExtension base = project.getExtensions().getByType(BasePluginExtension.class);
        base.getArchivesName().set("axon-" + project.getName());

        JavaPluginExtension javaExtension = project.getExtensions().getByType(JavaPluginExtension.class);
        javaExtension.withSourcesJar();
        javaExtension.withJavadocJar();

        // Matches root pom.xml's own "javadoc" profile (<doclint>none</doclint>) - Axon's real source uses
        // HTML the stricter doclint added since JDK 8 rejects outright (self-closing <p/> tags throughout,
        // confirmed by a real javadoc failure: "error: self-closing element not allowed" on JDK 25's
        // javadoc). Upstream already made this call for its own javadoc generation; this replicates it
        // rather than hand-editing Axon's own source files to satisfy a stricter tool than upstream itself
        // builds with.
        project.getTasks().withType(Javadoc.class).configureEach(javadoc -> {
            StandardJavadocDocletOptions options = (StandardJavadocDocletOptions) javadoc.getOptions();
            options.addStringOption("Xdoclint:none", "-quiet");
        });

        // Several real Maven modules (common, conversion, messaging, eventsourcing, modelling, update) use
        // maven-jar-plugin's test-jar goal so other modules can depend on their test-scope helper classes
        // (fixtures, base test classes) via a <type>test-jar</type> dependency - deferred here until a real
        // consumer existed to verify it against (axon-update is the first: its pom.xml depends on
        // axon-common's test-jar). Centralized rather than repeated per module, since the mechanism is
        // identical everywhere it's needed. Exposed two ways: as a real artifact on the Maven publication
        // (for external consumers, matching Maven's own behavior) and as an outgoing "testArtifacts"
        // configuration (so sibling modules in this same build can depend on it directly via project(...)
        // without a publish-then-resolve round trip).
        TaskProvider<Jar> testJar = project.getTasks().register("testJar", Jar.class, jar -> {
            jar.getArchiveClassifier().set("test");
            SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
            jar.from(sourceSets.getByName("test").getOutput());
        });

        // Maven's test-jar goal is bound to the package phase, so it runs on every normal build/install -
        // matched here rather than leaving testJar only runnable on-demand.
        project.getTasks().named("assemble").configure(assemble -> assemble.dependsOn(testJar));

        Configuration testArtifacts = project.getConfigurations().create("testArtifacts", configuration -> {
            configuration.setCanBeResolved(false);
            configuration.setCanBeConsumed(true);
        });

        project.getArtifacts().add(testArtifacts.getName(), testJar);

        PublishingExtension publishing = project.getExtensions().getByType(PublishingExtension.class);
        publishing.getPublications().create("maven", MavenPublication.class, publication -> {
            publication.from(project.getComponents().getByName("java"));
            publication.artifact(testJar);
            // base.archivesName above only renames the jar file itself - MavenPublication's
            // artifactId still defaults to the raw project.name independently and needs setting
            // here too, confirmed by inspecting the actually-generated POM, not assumed from the
            // jar filename being right.
            publication.setArtifactId("axon-" + project.getName());

            publication.pom(pom -> {
                pom.getUrl().set("https://github.com/Terrence721/AxonFramework-Full");
                pom.licenses(licenses -> licenses.license(license -> {
                    license.getName().set("Apache 2.0");
                    license.getUrl().set("https://www.apache.org/licenses/LICENSE-2.0");
                }));
                pom.developers(developers -> developers.developer(developer -> {
                    developer.getName().set("Terrence Daniels");
                    developer.getEmail().set("terrence_daniels_35@yahoo.com");
                }));
                pom.scm(scm -> {
                    scm.getConnection().set("scm:git:https://github.com/Terrence721/AxonFramework-Full.git");
                    scm.getDeveloperConnection().set("scm:git:git@github.com:Terrence721/AxonFramework-Full.git");
                    scm.getUrl().set("https://github.com/Terrence721/AxonFramework-Full");
                });
                pom.issueManagement(issueManagement -> {
                    issueManagement.getSystem().set("GitHub");
                    issueManagement.getUrl().set("https://github.com/Terrence721/AxonFramework-Full/issues");
                });
            });
        });

        // Only sign when key material is actually present, so an ordinary local build (or a CI job that
        // only compiles/tests) never fails for lacking release credentials - mirrors upstream's own
        // "sign" Maven profile only activating during an actual release, not every build.
        Provider<String> signingKey = project.getProviders().environmentVariable("SIGNING_KEY");
        Provider<String> signingPassword = project.getProviders().environmentVariable("SIGNING_PASSWORD");
        if (signingKey.isPresent() && signingPassword.isPresent()) {
            SigningExtension signing = project.getExtensions().getByType(SigningExtension.class);
            signing.useInMemoryPgpKeys(signingKey.get(), signingPassword.get());
            signing.sign(publishing.getPublications().getByName("maven"));
        }
    }
}
