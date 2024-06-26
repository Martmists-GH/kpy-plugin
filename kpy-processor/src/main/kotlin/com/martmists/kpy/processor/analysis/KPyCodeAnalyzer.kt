package com.martmists.kpy.processor.analysis

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import com.martmists.kpy.processor.collected.KPyClass
import com.martmists.kpy.processor.collected.KPyFunction
import com.martmists.kpy.processor.collected.KPyProperty

class KPyCodeAnalyzer {
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

                is KSPropertyDeclaration -> {
                    // class members are still included here, filter them out
                    if (it.parentDeclaration == null) {
                        val module = modules[it.packageName.asString()]
                        module.properties.add(it.asKPyProperty())
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
            it.parentDeclaration == this && it.annotations.any { ann ->
                ann.shortName.asString() == "PyExport"
            }
        }

        val magic = getAllFunctions().filter {
            it.parentDeclaration == this && it.annotations.any { ann ->
                ann.shortName.asString() == "PyMagic"
            }
        }

        val props = getAllProperties().filter {
            it.parentDeclaration == this && it.annotations.any { ann ->
                ann.shortName.asString() == "PyHint"
            }
        }

        val parent = this.getAllSuperTypes().firstOrNull {
            val decl = it.declaration
            decl is KSClassDeclaration &&
                    decl.classKind == ClassKind.CLASS && decl.annotations.any { ann ->
                ann.shortName.asString() == "PyExport"
            }
        }?.declaration as? KSClassDeclaration

        val parentType = parent?.asKPy()

        return KPyClass(
            simpleName.asString(),
            getExportName() ?: simpleName.getShortName(),
            this,
            parentType,
            funcs.map { it.asKPy() }.toMutableList(),
            magic.map { it.asKPyMagic() }.toMutableList(),
            props.map { it.asKPyProperty() }.toMutableList(),
            getExportPriority() ?: 9999
        )
    }

    private fun KSFunctionDeclaration.asKPy() : KPyFunction {
        return KPyFunction(
            simpleName.asString(),
            getExportName() ?: simpleName.getShortName(),
            this,
            getExportPriority() ?: 9999
        )
    }

    private fun KSFunctionDeclaration.asKPyMagic() : KPyFunction {
        return KPyFunction(
            simpleName.asString(),
            getMagicName() ?: simpleName.getShortName(),
            this
        )
    }

    private fun KSPropertyDeclaration.asKPyProperty(): KPyProperty {
        return KPyProperty(
            simpleName.asString(),
            getExportName() ?: simpleName.asString(),
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
        return ann.getNamedArgument<String>("name", 0)
    }

    private fun KSAnnotated.getExportPriority(): Int? {
        val ann = annotations.firstOrNull { it.shortName.getShortName() == "PyExport" } ?: return null
        return ann.getNamedArgument<Int>("priority", 1)
    }

    private fun <T> KSAnnotation.getNamedArgument(name: String, index: Int) : T? {
        val byName = mutableMapOf<String, T>()
        val byIndex = mutableMapOf<Int, T>()

        @Suppress("UNCHECKED_CAST")
        arguments.forEachIndexed { idx, argument ->
            if (argument.name != null) {
                byName[argument.name!!.asString()] = argument.value as T
            } else {
                byIndex[idx] = argument.value as T
            }
        }

        return byName[name] ?: byIndex[index]
    }
}
