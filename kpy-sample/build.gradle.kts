import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("com.google.devtools.ksp")
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
}

kotlin {
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("py")
        hostOs == "Linux" -> linuxX64("py")
        isMingwX64 -> mingwX64("py")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    sourceSets {
        val pyMain by getting {
            dependencies {
                implementation(project(":kpy-library"))
            }

            kotlin.srcDir(buildDir.absolutePath + "/generated/ksp/native/pyMain/kotlin")
        }
    }

    nativeTarget.apply {
        val main by compilations.getting {

        }

        binaries {
            staticLib {
                binaryOptions["memoryModel"] = "experimental"
                freeCompilerArgs += listOf("-Xgc=cms")
            }
        }
    }
}

val setupMetadata by tasks.creating {
    actions.add {
        println("""
            |===METADATA START===
            |project_name = "${project.name}"
            |project_version = "${project.version}"
            |build_dir = "${buildDir.absolutePath.replace('\\', '/')}"
            |===METADATA END===
        """.trimMargin())
    }
}

ksp {
    arg("projectName", project.name)
}

dependencies {
    for (target in kotlin.targets) {
        if (target is KotlinNativeTarget) {
            add("ksp${target.name.capitalize()}", project(":kpy-processor"))
        }
    }
}
