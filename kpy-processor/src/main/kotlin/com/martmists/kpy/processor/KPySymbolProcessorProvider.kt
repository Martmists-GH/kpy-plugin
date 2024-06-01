package com.martmists.kpy.processor

import com.google.auto.service.AutoService
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.martmists.kpy.processor.ext.toSnakeCase

@AutoService(SymbolProcessorProvider::class)
class KPySymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): KPySymbolProcessor {
        return KPySymbolProcessor(environment.options["projectName"]?.toSnakeCase() ?: "PROJECT_NAME", environment.options["generateStubs"]?.toBooleanStrict() ?: false, environment.codeGenerator)
    }
}
