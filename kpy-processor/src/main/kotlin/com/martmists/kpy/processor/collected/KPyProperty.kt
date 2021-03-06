package com.martmists.kpy.processor.collected

import com.google.devtools.ksp.symbol.KSPropertyDeclaration

data class KPyProperty(
    val name: String,
    val exportName: String,
    val declaration: KSPropertyDeclaration,
)
