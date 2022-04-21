# KPy Plugin

The KPy gradle plugin allows you to write kotlin/native code and use it from python.

## Setup

Enable the plugin in your build.gradle.kts file:

```kotlin
plugins {
    kotlin("multiplatform") version "1.6.20"  // current compatible version
    id("com.martmists.kpy-plugin") version "0.0.1"
}

kotlin {
    // Use the current Host OS as target platform with configuration name "native"
    // The configuration name *must* be "native" for the plugin to work
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }
    
    nativeTarget.apply {
        // Configure the target here
    }
}
```

Use the following setup.py template:

```python
from platform import system
from setuptools import setup, Extension
from subprocess import Popen, PIPE

osname = platform.system()
debug = True

if osname == "Linux":
    gradle_bin = "./gradlew"
else:
    gradle_bin = "./gradlew.bat"

# Build the project
proc = subprocess.Popen([gradle_bin, "build"])
proc.wait()

# Fetch configuration from gradle task
proc = subprocess.Popen([gradle_bin, "setupMetadata"], stdout=subprocess.PIPE)
proc.wait()
output = proc.stdout.read().decode()
real_output = output.split("===METADATA START===")[1].split("===METADATA END===")[0]

# Apply the configuration
exec(real_output, globals(), locals())

def snake_case(name):
    return name.replace("-", "_").lower()

def extensions():
    folder = "debugStatic" if debug else "releaseStatic"

    native = Extension(snake_case(project_name),
                       sources=[f'{build_dir}/generated/ksp/native/nativeMain/resources/entrypoint.cpp'],
                       include_dirs=[f"{build_dir}/bin/native/{folder}/"],
                       library_dirs=[f"{build_dir}/bin/native/{folder}/"],
                       libraries=[snake_case(project_name)])

    return [native]

with open("README.md", "r") as fp:
    long_description = fp.read()

setup(
    name=snake_case(project_name),
    version=project_version,
    description=long_description,
    ext_modules=extensions(),
    packages=[],
)
```

## Configuration

You can pass additional metadata from gradle to setup.py using the `kpy` configuration.

```kotlin
kpy {
    metadata("my_key", "'my_value'")  // Note: the second parameter is an expression, and must be valid python.
}
```

Then you can simply use it as a variable in your setup.py:

```python
print(my_key)  // prints "my_value"
```
