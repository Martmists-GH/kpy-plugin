plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version "2.0.0"
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    maven("https://maven.martmists.com/releases")
}

dependencies {
    implementation(kotlin("gradle-plugin", "2.0.0"))
    implementation("com.google.devtools.ksp:symbol-processing-gradle-plugin:2.0.0-1.0.21")
    implementation("com.github.gmazzo.buildconfig:plugin:5.3.5")
    implementation("com.github.ben-manes:gradle-versions-plugin:0.51.0")
    implementation("se.patrikerdes:gradle-use-latest-versions-plugin:0.2.18")
    implementation("com.martmists.commons:commons-gradle:1.0.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("io.ktor:ktor-client-core:2.3.11")
    implementation("io.ktor:ktor-client-cio:2.3.11")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.11")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.11")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}
