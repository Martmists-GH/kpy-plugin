import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("com.github.gmazzo.buildconfig")
}

dependencies {
    kapt("com.google.auto.service:auto-service:1.0.1")
    implementation("com.google.auto.service:auto-service-annotations:1.0.1")

    implementation("com.google.devtools.ksp:symbol-processing-api:1.6.20-1.0.5")
}

buildConfig {
    packageName.set("com.martmists.kpy.cfg")

    buildConfigField("String", "VERSION", "\"${project.version}\"")
}

tasks {
    withType<KotlinCompile> {
        dependsOn("generateBuildConfig")

        kotlinOptions {
            freeCompilerArgs = listOf("-Xcontext-receivers")
        }
    }
}
