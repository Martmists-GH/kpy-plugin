package com.martmists.kpy.plugin

open class KPyExtension {
    internal val props = mutableMapOf<String, String>()

    var pyVersion: String = "3.9"

    fun metadata(name: String, value: String) {
        props[name] = value
    }
}
