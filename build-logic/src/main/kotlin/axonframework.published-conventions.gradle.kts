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

// For modules that publish to Maven Central, layering on top of axonframework.java-conventions.
// Covers what root pom.xml's central-publishing-maven-plugin, its "javadoc"/"sources" profiles,
// and its "sign" profile (maven-gpg-plugin) apply to every published module - all three of those
// profiles only activate during an actual upstream release build (maven-release-plugin's
// releaseProfiles=javadoc,sources,sign), which is why signing below is gated on credentials being
// present rather than always running. Central Portal upload itself is wired in settings.gradle.kts
// (the com.gradleup.nmcp.settings plugin auto-applies com.gradleup.nmcp here - no plugin needed
// in this file for that part).
//
// POM metadata deliberately does NOT carry over root pom.xml's AxonIQ organization, upstream
// GitHub SCM URLs, or its named AxonIQ developers/emails - this fork publishes under its own
// maintainer and repository, not upstream's identity.

plugins {
    id("axonframework.java-conventions")
    `maven-publish`
    signing
}

// Every Axon module's real Maven artifactId is "axon-<name>" (axon-common, axon-test-logging, ...),
// but Gradle's base.archivesName defaults to the project directory name ("common", "test-logging"),
// with no automatic "axon-" prefix. Centralized here rather than repeated per module, since every
// published module needs it and it's easy to silently forget on any one of them.
base {
    archivesName = "axon-${project.name}"
}

java {
    withSourcesJar()
    withJavadocJar()
}

// Matches root pom.xml's own "javadoc" profile (<doclint>none</doclint>) - Axon's real source uses
// HTML the stricter doclint added since JDK 8 rejects outright (self-closing <p/> tags throughout,
// confirmed by a real javadoc failure: "error: self-closing element not allowed" on JDK 25's
// javadoc). Upstream already made this call for its own javadoc generation; this replicates it
// rather than hand-editing Axon's own source files to satisfy a stricter tool than upstream itself
// builds with.
tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            // base.archivesName above only renames the jar file itself - MavenPublication's
            // artifactId still defaults to the raw project.name independently and needs setting
            // here too, confirmed by inspecting the actually-generated POM, not assumed from the
            // jar filename being right.
            artifactId = "axon-${project.name}"

            pom {
                url = "https://github.com/Terrence721/AxonFramework-Full"
                licenses {
                    license {
                        name = "Apache 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                    }
                }
                developers {
                    developer {
                        name = "Terrence Daniels"
                        email = "terrence_daniels_35@yahoo.com"
                    }
                }
                scm {
                    connection = "scm:git:https://github.com/Terrence721/AxonFramework-Full.git"
                    developerConnection = "scm:git:git@github.com:Terrence721/AxonFramework-Full.git"
                    url = "https://github.com/Terrence721/AxonFramework-Full"
                }
                issueManagement {
                    system = "GitHub"
                    url = "https://github.com/Terrence721/AxonFramework-Full/issues"
                }
            }
        }
    }
}

// Only sign when key material is actually present, so an ordinary local build (or a CI job that
// only compiles/tests) never fails for lacking release credentials - mirrors upstream's own
// "sign" Maven profile only activating during an actual release, not every build.
val signingKey = providers.environmentVariable("SIGNING_KEY")
val signingPassword = providers.environmentVariable("SIGNING_PASSWORD")
if (signingKey.isPresent && signingPassword.isPresent) {
    signing {
        useInMemoryPgpKeys(signingKey.get(), signingPassword.get())
        sign(publishing.publications["maven"])
    }
}
