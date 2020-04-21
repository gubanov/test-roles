import org.gradle.api.tasks.testing.logging.TestLogEvent.*

plugins {
    kotlin("plugin.spring")
    id("io.spring.dependency-management")
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":security:security-core"))
    implementation(project(":security:security-persistence"))

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("com.github.javafaker:javafaker:1.0.2")
}

springBoot {
    mainClassName = "org.gubanov.app.RolesAppMainKt"
}

tasks {
    test {
        testLogging.exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        testLogging.events = setOf(STANDARD_OUT, STANDARD_ERROR, FAILED, PASSED, SKIPPED)
    }
}