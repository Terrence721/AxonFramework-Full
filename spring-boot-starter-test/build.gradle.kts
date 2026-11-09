plugins { id("axonframework.published-conventions") }
dependencies {
    api(project(":spring"))
    api(project(":test")) {
        exclude(group = "org.apache.logging.log4j", module = "log4j-slf4j2-impl")
    }
    api(project(":spring-boot-starter"))
    // Spring Boot pins log4j 2.24.3 (affected log4j-api advisory); align with :test-logging.
    implementation(platform("org.apache.logging.log4j:log4j-bom:2.26.1"))
    implementation("org.springframework.boot:spring-boot-starter-test:3.5.16")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
}
