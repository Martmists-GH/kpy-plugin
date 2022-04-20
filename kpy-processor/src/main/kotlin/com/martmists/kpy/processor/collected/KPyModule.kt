package com.martmists.kpy.processor.collected

data class KPyModule(
    val name: String,
    val classes: MutableList<KPyClass> = mutableListOf(),
    val functions: MutableList<KPyFunction> = mutableListOf(),
    val children: MutableList<KPyModule> = mutableListOf()
) {
    fun isRoot() : Boolean {
        return '.' in name
    }
}
