package com.martmists.kpy.processor.analysis

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import com.martmists.kpy.processor.collected.KPyClass
import com.martmists.kpy.processor.collected.KPyFunction

class KPyCodeAnalyzer(private val projectName: String, private val codeGenerator: CodeGenerator) {
    fun collect(resolver: Resolver) : KPyModuleCache {
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

        resolver.getSymbolsWithAnnotation("kpy.annotations.PyModuleHook", inDepth = false).forEach {
            when (it) {
                is KSFunctionDeclaration -> {
                    val module = modules[it.packageName.asString()]
                    module.hooks.add(it)
                }
            }

        }

        return modules
    }

    private fun KSClassDeclaration.asKPy() : KPyClass {
        val funcs = getAllFunctions().filter {
            it.annotations.any {
                it.shortName.asString() == "PyExport"
            }
        }

        val magic = getAllFunctions().filter {
            it.annotations.any {
                it.shortName.asString() == "PyMagic"
            }
        }

        val parent = this.getAllSuperTypes().firstOrNull() {
            val decl = it.declaration
            decl is KSClassDeclaration &&
                    decl.classKind == ClassKind.CLASS && decl.annotations.any {
                it.shortName.asString() == "PyExport"
            }
        }?.declaration as? KSClassDeclaration

        val parentType = parent?.asKPy()

        return KPyClass(
            simpleName.asString(),
            getExportName() ?: simpleName.getShortName(),
            this,
            parentType,
            funcs.map { it.asKPy() }.toMutableList(),
            magic.map { it.asKPyMagic() }.toMutableList()
        )
    }

    private fun KSFunctionDeclaration.asKPy() : KPyFunction {
        return KPyFunction(
            simpleName.asString(),
            getExportName() ?: simpleName.getShortName(),
            this
        )
    }

    private fun KSFunctionDeclaration.asKPyMagic() : KPyFunction {
        return KPyFunction(
            simpleName.asString(),
            getMagicName() ?: simpleName.getShortName(),
            this
        )
    }

    private fun KSAnnotated.getMagicName() : String? {
        val ann = annotations.firstOrNull { it.shortName.getShortName() == "PyMagic" } ?: return null
        val arg = ann.arguments.firstOrNull() ?: return null
        return (arg.value as KSType).declaration.simpleName.getShortName().lowercase()
    }

    private fun KSAnnotated.getExportName() : String? {
        val ann = annotations.firstOrNull { it.shortName.getShortName() == "PyExport" } ?: return null
        val arg = ann.arguments.firstOrNull() ?: return null
        return arg.value as String?
    }
}
