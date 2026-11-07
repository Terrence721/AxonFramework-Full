plugins {
    id("axonframework.internal-conventions")
}

dependencies {
    // Core modules under test (black-box integration across modules)
    api(project(":common"))
    api(project(":conversion"))
    api(project(":messaging"))
    api(project(":modelling"))
    api(project(":eventsourcing"))
    api(project(":test"))
    api(project(":test-logging"))

    // Additional module families
    api(project(":reactor"))
    api(project(":kotlin"))
    api(project(":migration"))
    api(project(":metrics-micrometer"))

    // Reactive
    implementation("io.projectreactor:reactor-core:3.8.6")

    // Jackson
    implementation(platform("com.fasterxml.jackson:jackson-bom:2.22.3"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.core:jackson-core")

    // Database drivers for cross-store coverage
    implementation("com.h2database:h2:2.3.232")
    implementation("org.hsqldb:hsqldb:2.7.4")
    implementation("com.mysql:mysql-connector-j:9.6.0")
    implementation("com.oracle.database.jdbc:ojdbc11:23.26.0.0.0")
    implementation("com.mchange:c3p0:0.12.0")

    // Other runtime/test utilities common across Axon
    implementation("org.awaitility:awaitility:4.3.0")
    implementation("org.hamcrest:hamcrest:2.2")
    implementation("com.google.code.findbugs:jsr305:3.0.2")

    implementation("org.junit.jupiter:junit-jupiter-api:5.12.2")

    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.12.2")
}
