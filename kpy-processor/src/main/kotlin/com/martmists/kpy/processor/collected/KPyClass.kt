package com.martmists.kpy.processor.collected

import com.google.devtools.ksp.symbol.KSClassDeclaration

data class KPyClass(
    val name: String,
    val exportName: String,
    val declaration: KSClassDeclaration,
    val parent: KPyClass?,
    val functions: MutableList<KPyFunction> = mutableListOf(),
    val magic: MutableList<KPyFunction> = mutableListOf(),
)
