package com.martmists.kpy.processor

import com.google.auto.service.AutoService
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.martmists.kpy.processor.analysis.KPyCodeAnalyzer
import com.martmists.kpy.processor.generation.KPyCodeGenerator
import com.martmists.kpy.processor.generation.KPyStubGenerator

@AutoService(SymbolProcessor::class)
class KPySymbolProcessor(private val projectName: String, private val generateStubs: Boolean, private val codeGenerator: CodeGenerator) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val modules = KPyCodeAnalyzer(projectName, codeGenerator).collect(resolver)

        val rootModule = modules.all().filter {
            it.isRoot()
        }.also {
            if (it.size > 1) {
                throw IllegalStateException("Expected exactly one root module, but found ${it.size}")
            }
        }.firstOrNull() ?: return emptyList()

        val gen = KPyCodeGenerator(projectName, generateStubs)
        val stubs = KPyStubGenerator()
        with(codeGenerator) {
            gen.generate(rootModule)
            if (generateStubs) {
                stubs.generate(rootModule)
            }
        }

        return emptyList()
    }
}
