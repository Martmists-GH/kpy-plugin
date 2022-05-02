from platform import system
from setuptools import setup, Extension, find_packages
from subprocess import Popen, PIPE

osname = system()
debug = True

if osname == "Linux":
    gradle_bin = "../gradlew"
else:
    gradle_bin = "../gradlew.bat"

# Build the project
proc = Popen([gradle_bin, "build"])
proc.wait()

# Fetch configuration from gradle task
proc = Popen([gradle_bin, "setupMetadata"], stdout=PIPE)
proc.wait()
output = proc.stdout.read().decode()
real_output = output.split("===METADATA START===")[1].split("===METADATA END===")[0]

exec(real_output, globals(), locals())

print("name: " + project_name)
print("version: " + project_version)


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


with open("README.md", "r") as fh:
    long_description = fh.read()


attrs = {}

if has_stubs:
    stub_root = f'{build_dir}/generated/ksp/{target}/{target}Main/resources/'
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
