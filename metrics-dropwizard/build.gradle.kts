plugins { id("axonframework.published-conventions") }
dependencies {
    api(project(":messaging"))
    implementation("io.dropwizard.metrics5:metrics-core:5.0.7")
    implementation("io.dropwizard.metrics5:metrics-jmx:5.0.7")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
}
