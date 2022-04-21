import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
    `maven-publish`
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
        val main by compilations.getting
        val python by main.cinterops.creating { }

        binaries {
            staticLib {
                binaryOptions["memoryModel"] = "experimental"
            }
        }
    }

    targets.withType<KotlinNativeTarget> {
        binaries.all {
            binaryOptions["memoryModel"] = "experimental"
            freeCompilerArgs += listOf("-Xgc=cms")
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
with open("${cinteropDir}/python.def", "w") as fp:
    fp.write(template.format(
        INCLUDE_DIR=paths["platinclude"],
        LIB_DIR='/'.join(paths['platstdlib'].split('/')[:-1]),
        MIN_VERSION_HEX="0x${versionHex}"
    ))
        """.trim()
    )
    outputs.upToDateWhen { false }
}

val cinteropPythonNative by tasks.getting {
    dependsOn(generatePythonDef)
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
