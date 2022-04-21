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
class KPySymbolProcessor(private val projectName: String, private val codeGenerator: CodeGenerator) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val modules = KPyModuleCache()

        resolver.getSymbolsWithAnnotation("kpy.annotations.PyExport", inDepth = false).forEach {
            when (it) {
                is KSClassDeclaration -> {
                    val module = modules[it.packageName.asString()]
                    module.classes.add(it.asKPy())
                }

                is KSFunctionDeclaration -> {
                    // class members are still included here, filter them out
                    if (it.parentDeclaration == null) {
                        val module = modules[it.packageName.asString()]
                        module.functions.add(it.asKPy())
                    }
                }
            }
        }

        val rootModule = modules.all().filter {
            it.isRoot()
        }.also {
            if (it.size > 1) {
                throw IllegalStateException("Expected exactly one root module, but found ${it.size}")
            }
        }.firstOrNull() ?: return emptyList()

        val gen = KPyCodeGenerator(projectName)
        with(codeGenerator) {
            gen.generate(rootModule)
        }

        return emptyList()
    }

    private fun KSClassDeclaration.asKPy() : KPyClass {
        val funcs = getAllFunctions().filter {
            it.annotations.any {
                it.shortName.asString() == "PyExport"
            }
        }

        val parent = this.parentDeclaration?.let {
            val isExport = it.annotations.any {
                it.shortName.asString() == "PyExport"
            }
            (it as KSClassDeclaration).asKPy()
        }

        return KPyClass(
            simpleName.asString(),
            getExportName() ?: simpleName.getShortName(),
            this,
            parent,
            funcs.map { it.asKPy() }.toMutableList()
        )
    }

    private fun KSFunctionDeclaration.asKPy() : KPyFunction {
        return KPyFunction(
            simpleName.asString(),
            getExportName() ?: simpleName.getShortName(),
            this,
            false
        )
    }

    private fun KSAnnotated.getExportName() : String? {
        val ann = annotations.firstOrNull { it.shortName.getShortName() == "PyExport" } ?: return null
        val arg = ann.arguments.firstOrNull() ?: return null
        return arg.value as String?
    }
}
