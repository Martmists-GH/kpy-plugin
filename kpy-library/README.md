# KPy Library

This is the common library for KPy projects.

To build on Linux or Macos, run the following command:

```bash
# On Macos, replace grep with GNU grep

# Replace this with the version you're targeting locally
PYTHON_VERSION="3.9"
wget $(curl "https://packages.msys2.org/package/mingw-w64-x86_64-python$PYTHON_VERSION?repo=mingw64" | grep -Po "(?<=>)https://mirror.msys2.org/.+?.zst") -O pkg.tar.zst
tar --use-compress-program=unzstd -xvf pkg.tar.zst
rm pkg.tar.zst .BUILDINFO .MTREE .PKGINFO
```
