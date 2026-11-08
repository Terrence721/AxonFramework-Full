plugins { id("axonframework.published-conventions") }
dependencies {
    api(project(":eventsourcing"))
    api(project(":reactor"))

    implementation("org.hamcrest:hamcrest:2.2")
    implementation("org.awaitility:awaitility:4.3.0")
    implementation("jakarta.inject:jakarta.inject-api:2.0.1")
    implementation("jakarta.persistence:jakarta.persistence-api:3.2.0")
    implementation(platform("com.fasterxml.jackson:jackson-bom:2.22.3"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.core:jackson-core")
    implementation("org.slf4j:slf4j-api:2.0.18")

    implementation("org.junit.jupiter:junit-jupiter-api:6.1.3")

    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
}
