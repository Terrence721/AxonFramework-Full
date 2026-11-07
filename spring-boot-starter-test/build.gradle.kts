plugins { id("axonframework.published-conventions") }
dependencies {
    api(project(":spring"))
    api(project(":spring-boot-starter"))
    implementation("org.springframework.boot:spring-boot-starter-test:3.5.16")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
}
