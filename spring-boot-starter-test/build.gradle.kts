plugins { id("axonframework.published-conventions") }
dependencies {
    api(project(":spring"))
    api(project(":test")) {
        exclude(group = "org.apache.logging.log4j", module = "log4j-slf4j2-impl")
    }
    api(project(":spring-boot-starter"))
    implementation("org.springframework.boot:spring-boot-starter-test:3.5.16")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
}
