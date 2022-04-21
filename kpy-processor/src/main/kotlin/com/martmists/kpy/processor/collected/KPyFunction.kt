package com.martmists.kpy.processor.collected

import com.google.devtools.ksp.symbol.KSFunctionDeclaration

data class KPyFunction(
    val name: String,
    val exportName: String,
    val declaration: KSFunctionDeclaration,
    val isSpecial: Boolean
)
