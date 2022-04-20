package com.martmists.kpy.processor

import com.google.auto.service.AutoService
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.martmists.kpy.processor.collected.KPyClass
import com.martmists.kpy.processor.collected.KPyFunction

@AutoService(SymbolProcessor::class)
class KPySymbolProcessor(val codeGenerator: CodeGenerator) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val modules = KPyModuleCache()

        resolver.getSymbolsWithAnnotation("kpy.annotations.PyExport").forEach {
            when (it) {
                is KSClassDeclaration -> {
                    val module = modules[it.packageName.asString()]

                    module.classes.add(it.asKPy())
                }

                is KSFunctionDeclaration -> {
                    val module = modules[it.packageName.asString()]

                    module.functions.add(it.asKPy())
                }
            }
        }

        modules.all().filter {
            it.isRoot()
        }.forEach {
            // Handle children first to add as properties
        }

        return emptyList()
    }

    private fun KSClassDeclaration.asKPy() : KPyClass {
        val funcs = getAllFunctions().filter {
            it.annotations.any {
                it.shortName.asString() == "PyExport"
            }
        }

        return KPyClass(
            simpleName.asString(),
            getExportName() ?: simpleName.asString(),
            this,
            funcs.map { it.asKPy() }.toMutableList()
        )
    }

    private fun KSFunctionDeclaration.asKPy() : KPyFunction {
        return KPyFunction(
            simpleName.asString(),
            getExportName() ?: simpleName.asString(),
            this
        )
    }

    private fun KSAnnotated.getExportName() : String? {
        val ann = annotations.firstOrNull { it.shortName.getShortName() == "PyExport" } ?: return null
        val arg = ann.arguments.firstOrNull() ?: return null
        return arg.value as String?
    }
}
