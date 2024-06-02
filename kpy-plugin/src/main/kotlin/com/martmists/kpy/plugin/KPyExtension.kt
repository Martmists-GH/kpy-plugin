package com.martmists.kpy.plugin

import com.martmists.kpy.cfg.BuildConfig
import org.gradle.api.provider.*

abstract class KPyExtension {
    init {
        pyVersion.convention(PythonVersion.Py311)
        kpyVersion.convention(BuildConfig.VERSION)
        generateStubs.convention(true)
        target.convention(null as String?)
        moduleName.convention(null as String?)
    }

    abstract val props: MapProperty<String, String>

    // Version to target
    abstract val pyVersion: Property<PythonVersion>
    abstract val kpyVersion: Property<String>

    // Generates the native code under _{name} and adds python stubs
    abstract val generateStubs: Property<Boolean>

    // Native target by name, defaults to first alphabetically
    abstract val target: Property<String?>

    // Name of the root native package
    abstract val moduleName: Property<String?>

    @Suppress("unused")
    fun metadata(key: String, value: String) {
        props.put(key, value)
    }

    @Suppress("unused")
    fun metadata(vararg entries: Pair<String, String>) {
        props.putAll(entries.toMap())
    }
}
