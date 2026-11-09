plugins { id("axonframework.published-conventions") }
dependencies {
    api(project(":spring"))
    api(project(":reactor"))
    implementation(platform("org.springframework.data:spring-data-bom:2025.0.13"))
    implementation("org.springframework.data:spring-data-commons")
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.5.16"))
    // Spring Boot's BOM pins log4j 2.24.3, affected by a log4j-api advisory fixed in 2.25.5;
    // align with the 2.26.1 used by :test-logging.
    implementation(platform("org.apache.logging.log4j:log4j-bom:2.26.1"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.apache.avro:avro:1.12.0")
    implementation(platform("tools.jackson:jackson-bom:3.2.3"))
    implementation("tools.jackson.dataformat:jackson-dataformat-cbor")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-cbor")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
}

// additional optional dependencies resolved for compilation
dependencies {
    implementation("org.springframework:spring-tx:6.2.19")
    implementation("jakarta.persistence:jakarta.persistence-api:3.2.0")
    implementation("jakarta.validation:jakarta.validation-api:3.1.1")
    implementation("org.quartz-scheduler:quartz:2.5.2")
}
