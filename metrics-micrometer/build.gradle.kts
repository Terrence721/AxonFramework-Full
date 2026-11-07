plugins { id("axonframework.published-conventions") }
dependencies {
    api(project(":messaging"))
    implementation("io.micrometer:micrometer-core:1.17.0")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
}
