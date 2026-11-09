plugins { id("axonframework.published-conventions") }
dependencies {
    api(project(":messaging"))
    api(project(":eventsourcing"))
    implementation(project(":modelling"))

    api("org.springframework:spring-context:6.2.19")
    implementation("org.springframework:spring-messaging:6.2.19")
    implementation("org.springframework:spring-orm:6.2.19")
    implementation("org.springframework:spring-context-support:6.2.19")
    implementation(platform("org.springframework.data:spring-data-bom:2025.0.13"))
    implementation("org.springframework.data:spring-data-commons")
    implementation("org.springframework.security:spring-security-core:6.5.11")
    implementation("org.springframework.security:spring-security-config:6.5.11")

    // Conversion - Avro (optional in Maven)
    implementation("org.apache.avro:avro:1.12.0")

    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
}
