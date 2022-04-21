import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
    `maven-publish`
}

kotlin {
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")

    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64()
        hostOs == "Linux" -> linuxX64()
        isMingwX64 -> mingwX64()
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    sourceSets {
        val nativeMain by creating {

        }

        try {
            val linuxX64Main by getting {
                dependsOn(nativeMain)
            }
        } catch (e: Exception) {
            println("LinuxX64Main not found")
        }

        try {
            val mingwX64Main by getting {
                dependsOn(nativeMain)
            }
        } catch (e: Exception) {
            println("MingwX64Main not found")
        }

        try {
            val macosX64Main by getting {
                dependsOn(nativeMain)
            }
        } catch (e: Exception) {
            println("MacosX64Main not found")
        }
    }

    nativeTarget.apply {
        val main by compilations.getting {

        }
        val python by main.cinterops.creating {

        }

        binaries {
            staticLib {
                binaryOptions["memoryModel"] = "experimental"
                freeCompilerArgs += listOf("-Xgc=cms")
            }
        }
    }
}

buildConfig {
    packageName.set("com.martmists.kpy.cfg")

    buildConfigField("String", "VERSION", "\"${project.version}\"")
}

val generatePythonDef = tasks.create<Exec>("generatePythonDef") {
    val minPyVersion = "3.9.0"

    group = "interop"
    description = "Generate Python.def file"
    executable = "python3"

    val cinteropDir = "${project.projectDir.absolutePath}/src/nativeInterop/cinterop"
    val parts = minPyVersion.split(".").toMutableList()
    while (parts.size < 4) {
        parts.add("0")
    }
    val versionHex = parts.joinToString("") { it.toInt().toString(16) }

    args(
        "-c",
        """
import sysconfig
paths = sysconfig.get_paths()
template = '''
headers = Python.h
package = python
compilerOpts = -I"{INCLUDE_DIR}"
linkerOpts = -L"{LIB_DIR}" -l python3

---

struct KtPyObject {{
    PyObject base;
    void* ktObject;
}};

// Wrapper func for _PyUnicode_AsString macro
char* PyUnicode_AsString(PyObject* obj) {{
    return _PyUnicode_AsString(obj);
}}
'''.strip()
with open('${cinteropDir.replace('\\', '/')}/python.def', 'w') as fp:
    fp.write(template.format(
        INCLUDE_DIR=paths['platinclude'],
        LIB_DIR='/'.join(paths['platstdlib'].split('/')[:-1]),
        MIN_VERSION_HEX='0x${versionHex}'
    ))
        """.trim()
    )
    outputs.upToDateWhen { false }
}

for (target in listOf("Linux", "Macos", "Mingw")) {
    try {
        tasks.getByName("cinteropPython${target}X64") {
            dependsOn(generatePythonDef)
        }
    } catch (e: Exception) {
        println("cinteropPython${target}X64 not found")
    }
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
                version = System.getenv("GITHUB_SHA")!!
            }
        }
    }
}
