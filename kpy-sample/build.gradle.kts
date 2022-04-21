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
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
        val main by compilations.getting {
            dependencies {
                implementation(project(":kpy-library"))
            }
        }

        binaries {
            staticLib {
                binaryOptions["memoryModel"] = "experimental"
            }
        }
    }

    sourceSets.named("nativeMain") {
        kotlin.srcDir(buildDir.absolutePath + "/generated/ksp/native/nativeMain/kotlin")
    }
}

val setupMetadata by tasks.creating {
    actions.add {
        println("""
            |===METADATA START===
            |project_name = "${project.name}"
            |project_version = "${project.version}"
            |build_dir = "${buildDir.absolutePath}"
            |===METADATA END===
        """.trimMargin())
    }
}

ksp {
    arg("projectName", project.name)
}

dependencies {
    add("kspNative", project(":kpy-processor"))
}
