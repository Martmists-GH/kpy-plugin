package com.martmists.kpy.processor

import com.google.auto.service.AutoService
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

@AutoService(SymbolProcessorProvider::class)
class KPySymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): KPySymbolProcessor {
        return KPySymbolProcessor(environment.options["projectName"] ?: "PROJECT_NAME", environment.options["generateStubs"]?.toBooleanStrict() ?: false, environment.codeGenerator)
    }
}
