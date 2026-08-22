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

include("build:parent")
include("axon-framework-bom")

include("common")
include("conversion")
include("extensions")
include("eventsourcing")
include("messaging")
include("migration")
include("modelling")
include("test")
include("test-logging")
include("update")

include("integrationtests")

include("docs:_samples")

// Maven profile-gated modules (activated via -Dexamples / -Dcoverage property presence).
// Gradle equivalent: only include when the matching -P property is passed on the command line.
if (providers.gradleProperty("examples").isPresent) {
    include("examples")
}
if (providers.gradleProperty("coverage").isPresent) {
    include("build:coverage-report")
}
