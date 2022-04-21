package com.martmists.kpy.plugin

open class KPyExtension {
    internal val props = mutableMapOf<String, String>()

    fun metadata(name: String, value: String) {
        props[name] = value
    }
}
