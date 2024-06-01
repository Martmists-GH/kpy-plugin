import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("com.google.devtools.ksp")
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
}

val targetName = "py"

kotlin {
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64(targetName)
        hostOs == "Linux" -> linuxX64(targetName)
        isMingwX64 -> mingwX64(targetName)
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    sourceSets {
        getByName("${targetName}Main") {
            dependencies {
                implementation(project(":kpy-library"))
            }

            kotlin.srcDir(layout.buildDirectory.dir("generated/ksp/$targetName/${targetName}Main/kotlin"))
        }
    }

    nativeTarget.apply {
        val main by compilations.getting {

        }

        binaries {
            staticLib {
                compilerOptions {
                    optIn = listOf(
                        "kotlinx.cinterop.ExperimentalForeignApi",
                    )
                }
            }
        }
    }
}

val setupMetadata by tasks.creating {
    actions.add {
        println("""
            |===METADATA START===
            |project_name = "${project.name}"
            |module_name = "${project.name}"
            |project_version = "${project.version}"
            |target = "$targetName"
            |has_stubs = True
            |build_dir = "${layout.buildDirectory.asFile.get().absolutePath.replace('\\', '/')}"
            |===METADATA END===
        """.trimMargin())
    }
}

ksp {
    arg("projectName", project.name)
    arg("generateStubs", "true")
}

dependencies {
    for (target in kotlin.targets) {
        if (target is KotlinNativeTarget) {
            add("ksp${target.name.capitalize()}", project(":kpy-processor"))
        }
    }
}
