package com.martmists.kpy.processor.generation

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.martmists.kpy.processor.collected.KPyClass
import com.martmists.kpy.processor.collected.KPyFunction
import com.martmists.kpy.processor.collected.KPyModule
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
                            write("import _${module.name}\n\n")

                            for (function in module.functions) {
                                generate(function)
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

        val allObjects = module.children.map { it.key } + module.classes.map { it.exportName } + module.functions.map { it.exportName }

        write(allObjects.joinToString(" ", "\n__all__ = (", ")\n") { "'$it'," })
    }

    context(KPyModule, OutputStreamWriter)
    private fun generate(clazz: KPyClass) {


        val superclass = if (clazz.parent != null) {
            write("import ${clazz.parent.declaration.packageName.asString()}.${clazz.parent.exportName.lowercase()} as ${clazz.parent.exportName.lowercase()}\n")
            "${clazz.parent.exportName.lowercase()}.${clazz.parent.exportName}"
        } else null

        val constructorParams = clazz.declaration.primaryConstructor!!.parameters.withIndex().joinToString(", ") { it.value.name?.asString() ?: "arg${it.index}" }
        val constructorParamsComma = if (constructorParams.isEmpty()) "" else ", $constructorParams"

        write("""
            |import _${name.split('.').first()}
            |
            |class ${clazz.exportName}(_$name.${clazz.exportName}${superclass?.let { ", $it" } ?: ""}):
            |    def __init__(self$constructorParamsComma):
            |       super().__init__($constructorParams)
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
        val params = function.declaration.parameters.withIndex().joinToString(", ") { it.value.name?.asString() ?: "arg${it.index}" }
        val paramsComma = if (params.isNotEmpty()) ", $params" else ""

        val doc = if (function.declaration.docString != null) {
            "\n" + ("'''\n" +
            "${function.declaration.docString}\n" +
            "'''\n").prependIndent(" ".repeat(8))
        } else {
            ""
        }

        write("""
            |    def ${function.exportName}(self$paramsComma):$doc
            |        return super().${function.exportName}($params)
            |
            |
        """.trimMargin())
    }

    context(OutputStreamWriter)
    private fun generateMagic(function: KPyFunction) {
        val params = function.declaration.parameters.withIndex().joinToString(", ") { it.value.name?.asString() ?: "arg${it.index}" }
        val paramsComma = if (params.isNotEmpty()) ", $params" else ""
        val magicName = transformMagic(function.exportName)

        val doc = if (function.declaration.docString != null) {
            "\n" + ("'''\n" +
            "${function.declaration.docString}\n" +
            "'''\n").prependIndent(" ".repeat(8))
        } else {
            ""
        }

        write("""
            |    def $magicName(self$paramsComma):$doc
            |        return super().$magicName($params)
            |
            |
        """.trimMargin())
    }

    context(KPyModule, OutputStreamWriter)
    private fun generate(function: KPyFunction) {
        val params = function.declaration.parameters.withIndex().joinToString(", ") { it.value.name?.asString() ?: "arg${it.index}" }
        val doc = if (function.declaration.docString != null) {
            "\n" + ("'''\n" +
                    "${function.declaration.docString}\n" +
                    "'''\n").prependIndent(" ".repeat(4))
        } else {
            ""
        }

        write("""
            |def ${function.exportName}($params):$doc
            |    return _${name}.${function.exportName}($params)
            |
            |
        """.trimMargin())
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
}
