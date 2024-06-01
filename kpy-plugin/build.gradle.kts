import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    kotlin("jvm")
    id("com.gradle.plugin-publish") version "0.21.0"
    id("com.github.gmazzo.buildconfig")
}

buildDir = file("../build/kpy-plugin")

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleKotlinDsl())
    implementation(kotlin("stdlib"))
    implementation(kotlin("gradle-plugin"))
    implementation("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.0.0-1.0.21")
}

pluginBundle {
    website = "https://github.com/martmists-gh/kpy-plugin"
    vcsUrl = "https://github.com/martmists-gh/kpy-plugin"
    description = "KPy Plugin is a plugin to target the Python C API from Kotlin/Native."
    tags = listOf(
        "kotlin", "multiplatform", "native", "python"
    )

    mavenCoordinates {
        groupId = "com.martmists.kpy"
        artifactId = "kpy-plugin"
    }
}

gradlePlugin {
    plugins {
        create("kpy") {
            id = "com.martmists.kpy.kpy-plugin"
            displayName = "KPy Plugin"
            implementationClass = "com.martmists.kpy.plugin.KPyPlugin"
        }
    }
}

buildConfig {
    packageName.set("com.martmists.kpy.cfg")
    buildConfigField("String", "VERSION", "\"${project.version}\"")
}

tasks {
    withType<KotlinCompile> {
        dependsOn("generateBuildConfig")
    }
}
