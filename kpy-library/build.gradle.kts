import org.gradle.configurationcache.extensions.capitalized
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.konan.target.KonanTarget

plugins {
    kotlin("multiplatform")
    `maven-publish`
    id("com.github.gmazzo.buildconfig")
}

val libVersion = version
val pythonVersion = let {
    val versionString = findProperty("pythonVersion") as String? ?: "3.11"
    DownloadPythonTask.Version.values().first { it.str == versionString }
}
version = "$version+${pythonVersion.str}"

kotlin {
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")

    val targets = listOfNotNull(
        if (hostOs == "Mac OS X") macosX64() else null,
        if (!isMingwX64) linuxX64() else null,
        mingwX64()
    )


    targets.forEach {
        it.apply {
            val main by compilations.getting {

            }
            val python by main.cinterops.creating {
                definitionFile = project.layout.projectDirectory.file("src/commonMain/cinterop/python.def")

                val downloadSourcesTask = tasks.register("downloadPython${targetName}", DownloadPythonTask::class) {
                    version = pythonVersion
                    platform = when (konanTarget) {
                        KonanTarget.MINGW_X64 -> DownloadPythonTask.Platform.Windows
                        KonanTarget.LINUX_X64 -> DownloadPythonTask.Platform.Linux
                        else -> throw IllegalArgumentException("Unsupported target: $targetName")
                    }
                }

                val extractSourcesTask = tasks.register("extractPython${targetName.capitalized()}", Exec::class) {
                    dependsOn(downloadSourcesTask)

                    val outDir =
                        project.layout.buildDirectory.dir("python-${pythonVersion.str}-${targetName}").get().asFile

                    executable = "tar"
                    args(
                        "-xzf",
                        downloadSourcesTask.get().tarFile.get().absolutePath,
                        "-C",
                        outDir.absolutePath
                    )

                    outputs.dir(outDir)

                    when (konanTarget) {
                        KonanTarget.MINGW_X64 -> {
                            includeDirs(outDir.resolve("python/include"))
                            linkerOpts("-L${outDir.resolve("python").absolutePath} -lpython3")
                        }

                        KonanTarget.LINUX_X64 -> {
                            includeDirs(outDir.resolve("python/include/python${pythonVersion.str}"))
                            linkerOpts("-L${outDir.resolve("python/lib").absolutePath} -lpython3 -lresolv")
                        }

                        else -> throw IllegalArgumentException("Unsupported target: $targetName")
                    }
                }

                tasks.named(interopProcessingTaskName) {
                    dependsOn(extractSourcesTask)
                }
            }
        }
    }
}

tasks {
    withType<KotlinNativeCompile> {
        compilerOptions {
            optIn = listOf(
                "kotlinx.cinterop.ExperimentalForeignApi",
                "kotlinx.cinterop.ExperimentalUnsignedTypes",
                "kotlinx.cinterop.UnsafeNumber",
            )
        }
    }
}

buildConfig {
    packageName.set("com.martmists.kpy.cfg")

    buildConfigField("String", "VERSION", "\"${libVersion}\"")
    buildConfigField("String", "PYTHON_VERSION", "\"${pythonVersion.str}\"")
}

if (project.ext.has("mavenToken")) {
    publishing {
        repositories {
            maven {
                name = "Host"
                url = uri("https://maven.martmists.com/releases")
                credentials {
                    username = "admin"
                    password = project.ext["mavenToken"]!! as String
                }
            }
        }

        publications.withType<MavenPublication> {

        }
    }
} else if (System.getenv("CI") == "true") {
    publishing {
        repositories {
            maven {
                name = "Host"
                url = uri(System.getenv("GITHUB_TARGET_REPO")!!)
                credentials {
                    username = "kpy-actions"
                    password = System.getenv("DEPLOY_KEY")!!
                }
            }
        }

        publications.withType<MavenPublication> {
            if (System.getenv("DEPLOY_TYPE") == "snapshot") {
                version = "${System.getenv("GITHUB_SHA")}+$pythonVersion"
            }
        }
    }
}
