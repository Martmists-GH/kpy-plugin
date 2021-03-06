# KPy Plugin

The KPy gradle plugin allows you to write Kotlin/Native code and use it from python.

> Note: Modules built with KPy still require XCode when building on macOS, this is a Kotlin/Native limitation.

For issues and future plans, refer to the [KPy YouTrack](https://youtrack.martmists.com/issues/KPY)

## Features

### Implemented

- Export Kotlin/Native functions and classes without having to touch the Python API directly
- Convert between Kotlin and Python types with .toPython() and .toKotlin()
- Conversions handled mostly automatically
- Class inheritance mapped to python
- Generate Python stubs
- Catch Kotlin exceptions and raise them as Python exceptions

## Setup

Change your gradle version to 7.5 (nightly builds only as of writing)
Enable the plugin in your build.gradle.kts file:

```kotlin
plugins {
    kotlin("multiplatform") version "1.6.21"  // current compatible version
    id("com.martmists.kpy.kpy-plugin") version "0.3.2"  // Requires Gradle 7.5+
}

kotlin {
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    // You can rename the target from `native` to something else, 
    // but make sure to also change setup.py to match this change!
    // ARM targets are also supported, but I don't know how to test for them
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    // Optional: Better memory manager
    nativeTarget.apply {
        binaries {
            staticLib {
                binaryOptions["memoryModel"] = "experimental"
                freeCompilerArgs += listOf("-Xgc=cms")
            }
        }
    }
}
```

Use the following setup.py template:

```python
from os.path import dirname, abspath
from platform import system
from setuptools import setup, Extension, find_packages
from subprocess import Popen, PIPE

osname = system()
debug = True
dir_name = dirname(abspath(__file__))

if osname == "Linux" or osname == "Darwin":
    gradle_bin = "./gradlew"
else:
    gradle_bin = ".\\gradlew.bat"

# Build the project
proc = Popen([gradle_bin, "build"])
if proc.wait() != 0:
    raise Exception("Build failed")

# Fetch configuration from gradle task
proc = Popen([gradle_bin, "setupMetadata"], stdout=PIPE)
if proc.wait() != 0:
    raise Exception("Failed to fetch metadata")
output = proc.stdout.read().decode()
real_output = output.split("===METADATA START===")[1].split("===METADATA END===")[0]

# Apply the configuration
exec(real_output, globals(), locals())
has_stubs: bool
project_name: str
project_version: str
build_dir: str
root_dir: str
target: str

# Prevent problems with multiple source roots, if able
build_dir = build_dir[len(dir_name)+1:]
root_dir = root_dir[len(dir_name)+1:]

def snake_case(name):
    return name.replace("-", "_").lower()


def extensions():
    folder = "debugStatic" if debug else "releaseStatic"
    prefix = "_" if has_stubs else ""
    native = Extension(prefix + snake_case(project_name),
                       sources=[f'{build_dir}/generated/ksp/{target}/{target}Main/resources/entrypoint.cpp'],
                       include_dirs=[f"{build_dir}/bin/{target}/{folder}/"],
                       library_dirs=[f"{build_dir}/bin/{target}/{folder}/"],
                       libraries=[snake_case(project_name)])

    return [native]


with open("README.md", "r") as fp:
    long_description = fp.read()


attrs = {}

if has_stubs:
    stub_root = f'{build_dir}/generated/ksp/{target}/{target}Main/resources'
    attrs["packages"] = find_packages(where=stub_root)
    attrs["package_dir"] = {"": stub_root}
else:
    attrs["packages"] = []

    
setup(
    name=snake_case(project_name),
    version=project_version,
    description=long_description,
    ext_modules=extensions(),
    **attrs
)
```

## Configuration

To configure the plugin, you can use the `kpy` configuration.

```kotlin
kpy {
    // Pass properties to setup.py, the exec() command will pass them to the context
    // Note: the second parameter is an expression, and must be valid python.
    metadata("my_key", "'my' + 'value'")  // in setup.py you can now use my_key and it evaluates to 'myvalue'

    // Specify the python version to build against.
    // Currently supported: [3.9, 3.10]
    pyVersion = "3.9"

    // Generate python stubs for the native sources
    // These are stored to `build/generated/ksp/<target>/<target>Main/resources/`
    // Note: these will be overwritten every time you build the project
    generateStubs = true
}
```
