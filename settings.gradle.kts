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

// pluginManagement must be the first block in a settings file - this is what makes convention
// plugin IDs defined in build-logic/ resolvable via plugins { id("...") } in module build scripts.
pluginManagement {
    includeBuild("build-logic")
}

// Mirrors the <modules> list of pom.xml (source: F:\AxonFramework-main\AxonFramework-main\pom.xml).
// rootProject.name = "axon" matches the Maven root artifactId so the root project keeps
// publishing at org.axonframework:axon without needing a base.archivesName override.
rootProject.name = "axon"

dependencyResolutionManagement {
    repositories {
        mavenCentral()

        // Mirrors pom.xml's <repositories> entry: releases disabled, snapshots enabled.
        // Also the Phase 5 publish target for SNAPSHOT versions of this project.
        maven {
            name = "centralPortalSnapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
            mavenContent { snapshotsOnly() }
        }
    }
}

// Commented out until each module directory actually exists - Gradle refuses to configure the
// build at all (not just the missing module) if an include(...) target doesn't exist on disk,
// which broke the wrapper task and the IDE's Gradle sync alike. Uncomment each line as that
// module is converted, per the bottom-up migration order in todo.md's "Still to do" section.
// include("build:parent")
// include("axon-framework-bom")

// include("common")
// include("conversion")
// include("extensions")
// include("eventsourcing")
// include("messaging")
// include("migration")
// include("modelling")
// include("test")
// include("test-logging")
// include("update")

// include("integrationtests")

// include("docs:_samples")

// Maven profile-gated modules (activated via -Dexamples / -Dcoverage property presence).
// Gradle equivalent: only include when the matching -P property is passed on the command line.
if (providers.gradleProperty("examples").isPresent) {
    include("examples")
}
if (providers.gradleProperty("coverage").isPresent) {
    include("build:coverage-report")
}
