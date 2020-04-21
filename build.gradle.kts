import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "1.3.71"
    kotlin("jvm") version kotlinVersion apply false
    kotlin("plugin.spring") version kotlinVersion apply false
    id("io.spring.dependency-management") version "1.0.9.RELEASE" apply false
    id("org.springframework.boot") version "2.2.6.RELEASE" apply false
}

subprojects {
    group = "org.gubanov.roles"
    version = "1.0-SNAPSHOT"

    repositories {
        jcenter()
    }

    apply {
        plugin("org.jetbrains.kotlin.jvm")
    }
    val implementation by configurations
    val testImplementation by configurations
    dependencies {
        implementation(kotlin("stdlib-jdk8"))
        implementation(kotlin("reflect"))

        implementation(platform("org.testcontainers:testcontainers-bom:1.14.0"))
        testImplementation("junit:junit:4.12")
        testImplementation("io.mockk:mockk:1.9.3")
        testImplementation("com.willowtreeapps.assertk:assertk:0.21")
        testImplementation("org.testcontainers:postgresql")
    }

    val compileKotlin: KotlinCompile by tasks
    compileKotlin.kotlinOptions {
        jvmTarget = "1.8"
    }
    val compileTestKotlin: KotlinCompile by tasks
    compileTestKotlin.kotlinOptions {
        jvmTarget = "1.8"
    }
}