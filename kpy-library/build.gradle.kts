import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.KonanTarget

plugins {
    kotlin("multiplatform")
    `maven-publish`
    id("com.github.gmazzo.buildconfig")
}

kotlin {
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val hostTarget = when {
        hostOs == "Mac OS X" -> KonanTarget.MACOS_X64
        hostOs == "Linux" -> KonanTarget.LINUX_X64
        isMingwX64 -> KonanTarget.MINGW_X64
        else -> error("Unsupported host OS: $hostOs")
    }

    fun KotlinNativeTarget.disableWin() = if (isMingwX64) null else this
    fun KotlinNativeTarget.disableLinux() = if (hostTarget == KonanTarget.LINUX_X64) null else this
    fun KotlinNativeTarget.disableMac() = if (hostTarget == KonanTarget.MACOS_X64) null else this

    val targets = listOf(
        // X64
        mingwX64(),

        linuxX64().disableWin(),
        macosX64().disableWin(),

        // Arm
        linuxArm64().disableWin(),
        macosArm64().disableWin(),
    ).filterNotNull()

    sourceSets {
        val nativeMain by creating {

        }

        val linuxX64Main by getting {
            dependsOn(nativeMain)
        }

        val mingwX64Main by getting {
            dependsOn(nativeMain)
        }

        val macosX64Main by getting {
            dependsOn(nativeMain)
        }

        val linuxArm64Main by getting {
            dependsOn(nativeMain)
        }

        val macosArm64Main by getting {
            dependsOn(nativeMain)
        }
    }

    targets.forEach {
        it.apply {
            val main by compilations.getting {

            }
            val python by main.cinterops.creating {
                if (konanTarget != hostTarget && konanTarget == KonanTarget.MINGW_X64) {
                    defFile = project.file("src/nativeInterop/cinterop/python-github-MingwX64.def")
                }
            }

            binaries {
                staticLib {
                    binaryOptions["memoryModel"] = "experimental"
                    freeCompilerArgs += listOf("-Xgc=cms")
                }
            }
        }
    }
}

val pyVersion = findProperty("pythonVersion") as String? ?: "3.9"
version = "$version+$pyVersion"

buildConfig {
    packageName.set("com.martmists.kpy.cfg")

    buildConfigField("String", "VERSION", "\"${project.version}\"")
}

val generatePythonDef = tasks.create<Exec>("generatePythonDef") {
    val minPyVersion = pyVersion

    group = "interop"
    description = "Generate Python.def file"
    executable = "python"

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
from platform import win32_edition
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

body = template.format(
    INCLUDE_DIR=paths['platinclude'],
    LIB_DIR='/'.join(paths['platstdlib'].split('/')[:-1]),
    MIN_VERSION_HEX='0x${versionHex}'
)
with open('${cinteropDir.replace('\\', '/')}/python.def', 'w') as fp:
    if win32_edition() is not None:
        body = body.replace('/', '\\')
    fp.write(body)
    print('${cinteropDir.replace('\\', '/')}/python.def\n' + body)

with open('${cinteropDir.replace('\\', '/')}/python-github-MingwX64.def', 'w') as fp:
    body = template.format(
        INCLUDE_DIR='${project.projectDir.absolutePath}/mingw64/include/python${pyVersion}',
        LIB_DIR='/'.join(paths['platstdlib'].split('/')[:-1]),
        MIN_VERSION_HEX='0x${versionHex}'
    )
    fp.write(body)
    print('${cinteropDir.replace('\\', '/')}/python-github-MingwX64.def\n' + body)
        """.trim()
    )
    files(
        "${cinteropDir.replace('\\', '/')}/python.def",
        "${cinteropDir.replace('\\', '/')}/python-windows.def",
        "${cinteropDir.replace('\\', '/')}/python-github-MingwX64.def",
    )
}

for (target in listOf("LinuxX64", "MacosX64", "MingwX64", "LinuxArm64", "MacosArm64")) {
    try {
        val interop = tasks.getByName("cinteropPython${target}") {
            dependsOn(generatePythonDef)
        }
        tasks.named("generateProjectStructureMetadata") {
            dependsOn(interop)
        }
    } catch (e: Exception) {
        println("Skipping cinteropPython${target} as it's not available on this OS")
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
                version = "${System.getenv("GITHUB_SHA")}+$pyVersion"
            }
        }
    }
}
