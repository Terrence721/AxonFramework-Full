plugins {
    id("axonframework.published-conventions")
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
}

dependencies {
    api(project(":messaging"))
    api(project(":modelling"))
    api(project(":eventsourcing"))
    implementation(project(":reactor"))

    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.3.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.3.0")
    implementation("io.projectreactor:reactor-core:3.8.6")

    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.3.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
}


tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25)
}


tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach { useJUnitPlatform() }
