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
}

dependencies {
    add("kspNative", project(":kpy-processor"))
}
