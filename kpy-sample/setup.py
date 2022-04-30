from setuptools import setup, Extension
import subprocess

debug = True

# Build the project
proc = subprocess.Popen(['../gradlew', 'build'])
if proc.wait() != 0:
    raise Exception("Gradle build failed with non-zero exit code")

# Fetch configuration
proc = subprocess.Popen(["../gradlew", "setupMetadata"], stdout=subprocess.PIPE)
if proc.wait() != 0:
    raise Exception("Gradle build failed with non-zero exit code")

output = proc.stdout.read().decode()
real_output = output.split("===METADATA START===")[1].split("===METADATA END===")[0]

exec(real_output, globals(), locals())

print("name: " + project_name)
print("version: " + project_version)

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

with open("README.md", "r") as fh:
    long_description = fh.read()

setup(
    name=snake_case(project_name),
    version=project_version,
    description=long_description,
    ext_modules=extensions(),
    packages=[],
)
