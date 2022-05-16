package com.martmists.kpy.processor.generation

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSValueParameter
import com.martmists.kpy.processor.collected.KPyClass
import com.martmists.kpy.processor.collected.KPyFunction
import com.martmists.kpy.processor.collected.KPyModule
import com.martmists.kpy.processor.collected.KPyProperty
import java.io.OutputStreamWriter

class KPyStubGenerator {
    context(CodeGenerator)
    fun generate(module: KPyModule) {
        for (child in module.children) {
            generate(child)
        }

        // Generate classes
        with (module) {
            for (clazz in module.classes) {
                createNewFile(
                    Dependencies(true, *module.files().toTypedArray()),
                    module.name,
                    clazz.exportName.lowercase(),
                    "py"
                ).use { stream ->
                    OutputStreamWriter(stream).use {
                        with(it) {
                            generate(clazz)
                        }
                    }
                }
            }

            if (module.functions.isNotEmpty()) {
                createNewFile(
                    Dependencies(true, *module.files().toTypedArray()),
                    module.name,
                    "functions",
                    "py"
                ).use { stream ->
                    OutputStreamWriter(stream).use {
                        with(it) {
                            write("""
                                |import _${module.name}
                                |from typing import Any, Dict, List, Optional, Tuple
                                |
                                |
                            """.trimMargin())

                            for (function in module.functions) {
                                generate(function)
                            }
                        }
                    }
                }
            }

            if (module.properties.isNotEmpty()) {
                createNewFile(
                    Dependencies(true, *module.files().toTypedArray()),
                    module.name,
                    "properties",
                    "py"
                ).use { stream ->
                    OutputStreamWriter(stream).use {
                        with(it) {
                            write("""
                                |import _${module.name}
                                |from typing import Any, Dict, List, Optional, Tuple
                                |
                                |
                            """.trimMargin())

                            for (property in module.properties) {
                                generate(property)
                            }
                        }
                    }
                }
            }
        }

        createNewFile(Dependencies(true, *module.files().toTypedArray()), module.name, "__init__", "py").use { stream ->
            OutputStreamWriter(stream).use {
                with(it) {
                    generateModule(module)
                }
            }
        }
    }

    context(OutputStreamWriter)
    fun generateModule(module: KPyModule) {
        for (child in module.children) {
            write("import ${child.name} as ${child.key}\n")
        }
        for (clazz in module.classes) {
            write("from .${clazz.exportName.lowercase()} import ${clazz.exportName}\n")
        }
        if (module.functions.isNotEmpty()) {
            write("from .functions import ${module.functions.joinToString(", ") { it.exportName }}\n")
        }
        if (module.properties.isNotEmpty()) {
            write("from .properties import ${module.properties.joinToString(", ") { it.exportName }}\n")
        }

        val allObjects = module.children.map { it.key } + module.classes.map { it.exportName } + module.functions.map { it.exportName } + module.properties.map { it.exportName }

        write(allObjects.joinToString(" ", "\n__all__ = (", ")\n") { "'$it'," })
    }

    context(KPyModule, OutputStreamWriter)
    private fun generate(clazz: KPyClass) {
        val superclass = if (clazz.parent != null) {
            write("import ${clazz.parent.declaration.packageName.asString()}.${clazz.parent.exportName.lowercase()} as ${clazz.parent.exportName.lowercase()}\n")
            "${clazz.parent.exportName.lowercase()}.${clazz.parent.exportName}"
        } else null
        val doc = if (clazz.declaration.docString != null) {
            "\n" + ("'''\n" +
                    "${clazz.declaration.docString?.trim()}\n" +
                    "'''\n\n").prependIndent(" ".repeat(4))
        } else {
            ""
        }
        val props = if (clazz.properties.isNotEmpty()) {
            "\n" + clazz.properties.joinToString("\n") {
                "${it.name}: ${remapType(it.declaration.type.resolve())}".prependIndent(" ".repeat(4))
            } + "\n"
        } else {
            ""
        }

        val constructorParams = clazz.declaration.primaryConstructor!!.parameters.withIndex().joinToString(", ") { remapParam(it.value, it.index) }
        val constructorParamsNoType = clazz.declaration.primaryConstructor!!.parameters.withIndex().joinToString(", ") { it.value.name?.asString() ?: "arg${it.index}" }
        val constructorParamsComma = if (constructorParams.isEmpty()) "" else ", $constructorParams"

        write("""
            |import _${name.split('.').first()}
            |from typing import Any, Dict, List, Optional, Tuple
            |
            |class ${clazz.exportName}(_$name.${clazz.exportName}${superclass?.let { ", $it" } ?: ""}):$doc$props
            |    def __init__(self$constructorParamsComma):
            |       super().__init__($constructorParamsNoType)
            |
            |
        """.trimMargin())

        for (magic in clazz.magic) {
            generateMagic(magic)
        }

        for (function in clazz.functions) {
            generateMethod(function)
        }
    }

    context(OutputStreamWriter)
    private fun generateMethod(function: KPyFunction) {
        val params = function.declaration.parameters.withIndex().joinToString(", ") { remapParam(it.value, it.index) }
        val paramsNoType = function.declaration.parameters.withIndex().joinToString(", ") { it.value.name?.asString() ?: "arg${it.index}" }
        val paramsComma = if (params.isNotEmpty()) ", $params" else ""

        val doc = if (function.declaration.docString != null) {
            "\n" + ("'''\n" +
            "${function.declaration.docString?.trim()}\n" +
            "'''\n").prependIndent(" ".repeat(8))
        } else {
            ""
        }

        write("""
            |    def ${function.exportName}(self$paramsComma) -> ${remapType(function.declaration.returnType!!.resolve())}:$doc
            |        return super().${function.exportName}($paramsNoType)
            |
            |
        """.trimMargin())
    }

    context(OutputStreamWriter)
    private fun generateMagic(function: KPyFunction) {
        val params = function.declaration.parameters.withIndex().joinToString(", ") { remapParam(it.value, it.index) }
        val paramsNoType = function.declaration.parameters.withIndex().joinToString(", ") { it.value.name?.asString() ?: "arg${it.index}" }
        val paramsComma = if (params.isNotEmpty()) ", $params" else ""
        val magicName = transformMagic(function.exportName)

        val doc = if (function.declaration.docString != null) {
            "\n" + ("'''\n" +
            "${function.declaration.docString?.trim()}\n" +
            "'''\n").prependIndent(" ".repeat(8))
        } else {
            ""
        }

        write("""
            |    def $magicName(self$paramsComma) -> ${remapType(function.declaration.returnType!!.resolve())}:$doc
            |        return super().$magicName($paramsNoType)
            |
            |
        """.trimMargin())
    }

    context(KPyModule, OutputStreamWriter)
    private fun generate(function: KPyFunction) {
        val params = function.declaration.parameters.withIndex().joinToString(", ") { remapParam(it.value, it.index) }
        val paramsNoType = function.declaration.parameters.withIndex().joinToString(", ") { it.value.name?.asString() ?: "arg${it.index}" }
        val doc = if (function.declaration.docString != null) {
            "\n" + ("'''\n" +
                    "${function.declaration.docString?.trim()}\n" +
                    "'''\n").prependIndent(" ".repeat(4))
        } else {
            ""
        }

        write("""
            |def ${function.exportName}($params) -> ${remapType(function.declaration.returnType!!.resolve())}:$doc
            |    return _${name}.${function.exportName}($paramsNoType)
            |
            |
        """.trimMargin())
    }

    context(KPyModule, OutputStreamWriter)
    private fun generate(property: KPyProperty) {
        write("${property.exportName}: ${remapType(property.declaration.type.resolve())} = _${name}.${property.exportName}\n")
    }

    private fun transformMagic(name: String): String {
        return when (name) {
            "tp_getattro" -> "__getattr__"
            "tp_setattro" -> "__setattr__"
            "tp_richcompare" -> "__richcmp__"
            "tp_iter" -> "__iter__"
            "tp_iternext" -> "__next__"
            "tp_traverse" -> "__traverse__"
            "am_await" -> "__await__"
            "am_aiter" -> "__aiter__"
            "am_anext" -> "__anext__"
            "nb_absolute" -> "__abs__"
            "nb_add" -> "__add__"
            "nb_and" -> "__and__"
            "nb_bool" -> "__bool__"
            "nb_divmod" -> "__divmod__"
            "nb_float" -> "__float__"
            "nb_floor_divide" -> "__floordiv__"
            "nb_index" -> "__index__"
            "nb_inplace_add" -> "__iadd__"
            "nb_inplace_and" -> "__iand__"
            "nb_inplace_floor_divide" -> "__ifloordiv__"
            "nb_inplace_lshift" -> "__ilshift__"
            "nb_inplace_matrix_multiply" -> "__imatmul__"
            "nb_inplace_multiply" -> "__imul__"
            "nb_inplace_or" -> "__ior__"
            "nb_inplace_power" -> "__ipow__"
            "nb_inplace_remainder" -> "__irem__"
            "nb_inplace_rshift" -> "__irshift__"
            "nb_inplace_subtract" -> "__isub__"
            "nb_inplace_true_divide" -> "__itruediv__"
            "nb_inplace_xor" -> "__ixor__"
            "nb_int" -> "__int__"
            "nb_invert" -> "__invert__"
            "nb_lshift" -> "__lshift__"
            "nb_matrix_multiply" -> "__matmul__"
            "nb_multiply" -> "__mul__"
            "nb_negative" -> "__neg__"
            "nb_or" -> "__or__"
            "nb_positive" -> "__pos__"
            "nb_power" -> "__pow__"
            "nb_remainder" -> "__mod__"
            "nb_rshift" -> "__rshift__"
            "nb_subtract" -> "__sub__"
            "nb_true_divide" -> "__truediv__"
            "nb_xor" -> "__xor__"
            "sq_length" -> "__len__"
            "sq_concat" -> "__add__"
            "sq_repeat" -> "__mul__"
            "sq_item" -> "__getitem__"
            "sq_ass_item" -> "__setitem__"
            "sq_contains" -> "__contains__"
            "sq_inplace_concat" -> "__iadd__"
            "sq_inplace_repeat" -> "__imul__"
            "mp_length" -> "__len__"
            "mp_subscript" -> "__getitem__"
            "mp_ass_subscript" -> "__setitem__"
            else -> name
        }
    }

    private fun remapParam(param: KSValueParameter, index: Int): String {
        return "${param.name?.asString() ?: "arg$index"}: ${remapType(param.type.resolve())}"
    }

    private fun remapType(type: KSType) : String {
        val it = type.toString().removeSuffix("?")
        val newType = when {
            // Primitives
            it == "Int" -> "int"
            it == "Long" -> "int"
            it == "Float" -> "float"
            it == "Double" -> "float"
            it == "Boolean" -> "bool"
            it == "String" -> "str"
            it == "Unit" -> "None"

            // Array types
            it == "FloatArray" -> "List[float]"
            it == "DoubleArray" -> "List[float]"
            it == "IntArray" -> "List[int]"
            it == "LongArray" -> "List[int]"
            it == "ByteArray" -> "bytes"

            // PyObject* aka Any
            it == "CPointer<_object>" -> "Any"
            it == "Any" -> "Any"

            // Generic types
            it.startsWith("List<") -> "List[${remapType(type.arguments.first().type!!.resolve())}]"
            it.startsWith("Map<") -> "Dict[${type.arguments.joinToString(", ") { remapType(it.type!!.resolve()) }}]"
            it.startsWith("Pair<") || it.startsWith("Triple<") -> "Tuple[${type.arguments.joinToString(", ") { remapType(it.type!!.resolve()) }}]"

            // Type aliases
            it.startsWith("[typealias ") -> remapType((type.declaration as KSTypeAlias).type.resolve())
            else -> "'$it'"
        }
        return if (type.toString().endsWith('?')) "Optional[$newType]" else newType
    }
}
