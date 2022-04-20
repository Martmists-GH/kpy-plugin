import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-gradle-plugin`
    kotlin("jvm")
    id("com.gradle.plugin-publish")
    id("com.github.gmazzo.buildconfig")
}

dependencies {
    implementation(gradleKotlinDsl())
    implementation(kotlin("stdlib"))
    implementation(kotlin("gradle-plugin"))
    implementation("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:1.6.20-1.0.5")
}

gradlePlugin {
    plugins {
        create("kpy") {
            id = "com.martmists.kpy-plugin"
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

        kotlinOptions {
            freeCompilerArgs = listOf("-Xcontext-receivers")
        }
    }
}
