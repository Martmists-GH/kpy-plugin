package com.martmists.kpy.plugin

import com.martmists.kpy.cfg.BuildConfig

open class KPyExtension {
    internal val props = mutableMapOf<String, String>()

    // Version to target
    // Supported: [3.10]
    var pyVersion: PythonVersion = PythonVersion.Py310
    var kpyVersion: String = BuildConfig.VERSION

    // Generates the native code under _{name} and adds python stubs
    var generateStubs: Boolean = true

    // Native target by name, defaults to first alphabetically
    var target: String? = null

    // Add properties to propagate to setup.py
    fun metadata(name: String, value: String) {
        props[name] = value
    }
}
