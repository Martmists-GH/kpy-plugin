package com.martmists.kpy.processor.collected

import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

data class KPyModule(
    val name: String,
    val classes: MutableList<KPyClass> = mutableListOf(),
    val functions: MutableList<KPyFunction> = mutableListOf(),
    val properties: MutableList<KPyProperty> = mutableListOf(),
    val children: MutableList<KPyModule> = mutableListOf(),
    val hooks: MutableList<KSFunctionDeclaration> = mutableListOf(),
) {
    val key = name.split(".").last()

    fun isRoot() : Boolean {
        return '.' !in name
    }

    fun files() : List<KSFile> {
        val files = mutableSetOf<KSFile>()

        for (c in classes) {
            files.add(c.declaration.containingFile!!)
        }

        for (f in functions) {
            files.add(f.declaration.containingFile!!)
        }

        for (m in children) {
            files.addAll(m.files())
        }

        return files.toList()
    }
}
