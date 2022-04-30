package com.martmists.kpy.processor

import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.martmists.kpy.processor.collected.KPyClass
import com.martmists.kpy.processor.collected.KPyFunction
import com.martmists.kpy.processor.collected.KPyModule
import com.martmists.kpy.processor.ext.toKPyName
import com.martmists.kpy.processor.ext.toSnakeCase
import java.io.OutputStreamWriter

class KPyCodeGenerator(private val projectName: String) {
    context(CodeGenerator)
    fun generate(module: KPyModule) {
        for (child in module.children) {
            generate(child)
        }

        createNewFile(Dependencies(true, *module.files().toTypedArray()), module.name, "kpy-module", "kt").use { stream ->
            OutputStreamWriter(stream).use {
                with(it) {
                    write("package ${module.name}\n")

                    generateImports()

                    for (classDef in module.classes) {
                        generate(classDef)
                    }

                    for (function in module.functions) {
                        generate(function)
                    }

                    // Generate module, add funcdef/classes/children
                    generateModule(module)
                }
            }
        }

        if (module.isRoot()) {
            // cpp entrypoint
            createNewFile(Dependencies(true, *module.files().toTypedArray()), "", "entrypoint", "cpp").use { stream ->
                OutputStreamWriter(stream).use {
                    with(it) {
                        generateEntrypoint(module)
                    }
                }
            }

            // kt entrypoint
            createNewFile(Dependencies(true, *module.files().toTypedArray()), "", "entrypoint", "kt").use { stream ->
                OutputStreamWriter(stream).use {
                    with(it) {
                        generateKtEntrypoint(module)
                    }
                }
            }
        }
    }

    context(OutputStreamWriter)
    private fun generateModule(module: KPyModule) {
        val methods = if (module.functions.isEmpty()) {
            "null"
        } else {
            """
                |listOf(
                |        ${module.functions.joinToString(",\n") { "`${it.name}-kpy-def`" }}                
                |    )
            """.trimMargin()
        }

        write("""
            |private val mod = makeModule(
            |    km_name = "${module.name}",
            |    km_methods = $methods
            |)
            |
            |fun createModule() : PyObjectT {
            |    val obj = PyModule_Create2(mod.ptr, PYTHON_API_VERSION)
            |    ${module.hooks.joinToString("\n") { "${it.simpleName.asString()}(obj)" }}
            |    ${module.classes.joinToString("\n") { "if (obj.addType(KPyType_${it.exportName}) < 0) return null" }}
            |    ${module.children.joinToString("\n") { "if (PyModule_AddObject(obj, \"${it.key}\", ${it.name}.createModule().incref()) < 0) return null" }}
            |    return obj
            |}
            |
        """.trimMargin())
    }

    context(OutputStreamWriter)
    fun generateEntrypoint(module: KPyModule) {
        write("""
            |#include <Python.h>
            |#include "lib${projectName.toSnakeCase()}_api.h"
            |
            |extern "C" {
            |PyMODINIT_FUNC PyInit_${module.name}(void) {
            |    return (PyObject*)lib${projectName.toSnakeCase()}_symbols()->kotlin.root.initialize();
            |}
            |}
            |
        """.trimMargin())
    }

    context(OutputStreamWriter)
    fun generateKtEntrypoint(module: KPyModule) {
        write("""
            |import kotlinx.cinterop.CPointer
            |import python.PyObject
            |
            |fun initialize(): CPointer<PyObject>? {
            |    return ${module.name}.createModule()
            |}
            |
        """.trimMargin())
    }

    context(OutputStreamWriter)
    fun generateImports() {
        write("""
            |import kotlinx.cinterop.*
            |
            |import kpy.builders.*
            |import kpy.ext.*
            |import kpy.internals.*
            |import kpy.utilities.*
            |import kpy.wrappers.*
            |
            |import python.*
            |
            |
        """.trimMargin())
    }

    context(OutputStreamWriter)
    fun generate(clazz: KPyClass) {
        for (function in clazz.functions) {
            generateMethod(clazz, function)
        }

        val abstractBody = """
            |PyErr_SetString(
            |        PyExc_NotImplementedError,
            |        "Cannot instantiate abstract class ${clazz.exportName}."
            |    )
            |    -1
            |
        """.trimMargin()

        val constructor = clazz.declaration.primaryConstructor!!
        val implBody = if (constructor.parameters.isNotEmpty()) """
            |memScoped {
            |        val selfObj: CPointer<KtPyObject> = self?.reinterpret() ?: return@memScoped -1
            |        ${parseArgs("__init__", constructor)}
            |
            |        val instance = ${clazz.name}(
            |            ${params(constructor.parameters.map { it.type.resolve().isMarkedNullable })}
            |        )
            |        val ref = StableRef.create(instance)
            |        selfObj.pointed.ktObject = ref.asCPointer()
            |        self.remember(instance)
            |        0
            |    }
            |
        """.trimMargin() else """
            |memScoped {
            |        val selfObj: CPointer<KtPyObject> = self?.reinterpret() ?: return@memScoped -1
            |        val instance = ${clazz.name}()
            |        val ref = StableRef.create(instance)
            |        selfObj.pointed.ktObject = ref.asCPointer()
            |        0
            |    }
            |
        """.trimMargin()

        val parentClass = if (clazz.parent == null) "PyBaseObject_Type" else "${clazz.parent.declaration.packageName.asString()}.KPyType_${clazz.parent.name}"

        write("""
            |private val `${clazz.name}-kpy-init` = staticCFunction { self: PyObjectT, args: PyObjectT, kwargs: PyObjectT ->
            |    ${if (clazz.declaration.isAbstract()) abstractBody else implBody}
            |}
            |
            |private fun `kpy-parent-type`(): PyObjectT = `${parentClass}`.ptr.reinterpret()
            |
            |val KPyType_${clazz.name} = makePyType(
            |    name = "${clazz.declaration.packageName.asString()}.${clazz.exportName}",
            |    ktp_init = `${clazz.name}-kpy-init`,
            |    ktp_base = ${parentClass}.ptr,
            |    ktp_methods = listOf(
            |        ${clazz.functions.filter { !it.isSpecial }.joinToString(",\n") { "`${clazz.name}-${it.name}-kpy-def`" }}                
            |    ),
            |    ${clazz.functions.filter { it.isSpecial }.joinToString(",\n") { "${it.name.toKPyName()} = `${clazz.name}-${it.name.toKPyName()}`" }}
            |)._registerType<${clazz.name}>()
            |
        """.trimMargin())
    }

    context(OutputStreamWriter)
    fun generateMethod(clazz: KPyClass, function: KPyFunction) {
        val params = function.declaration.parameters
        val nargs = params.size
        val docstring = function.declaration.docString ?: ""

        val body = if (nargs != 0) """
            |${parseArgs(function.exportName, function.declaration)}
            |        val result = selfKt.${function.name}(
            |            ${params(params.map { it.type.resolve().isMarkedNullable })}
            |        )
        """.trimMargin() else """
            |val result = selfKt.${function.name}()
        """.trimMargin()

        write("""
            |private val `${clazz.name}-${function.name}-kpy-fun` = staticCFunction { self: PyObjectT, args: PyObjectT, kwargs: PyObjectT ->
            |    memScoped {
            |        val selfKt = self!!.kt.cast<${clazz.name}>()
            |        $body
            |        result.toPython().incref()
            |    }
            |}
            |
            |private val `${clazz.name}-${function.name}-kpy-def` = `${clazz.name}-${function.name}-kpy-fun`.pydef(
            |    "${function.exportName}", "${docstring.replace("\"", "\\\"")}", ${if (nargs == 0) "METH_NOARGS" else "METH_VARARGS or METH_KEYWORDS"}
            |)
            |
        """.trimMargin())
    }

    context(OutputStreamWriter)
    fun generate(function: KPyFunction) {
        val params = function.declaration.parameters
        val nargs = params.size

        val bodyParams = """
            |memScoped {
            |        ${parseArgs(function.exportName, function.declaration)}
            |        
            |        ${function.name}(
            |            ${params(params.map { it.type.resolve().isMarkedNullable })}
            |        ).toPython()
            |    }
            |
        """.trimMargin()

        val bodyNoParams = """
            |${function.name}().toPython()
            |
        """.trimMargin()

        val docstring = function.declaration.docString ?: ""

        write("""
            |private val `${function.name}-kpy-fun` = staticCFunction { self: PyObjectT, args: PyObjectT, kwargs: PyObjectT ->
            |    ${if (nargs == 0) bodyNoParams else bodyParams}
            |}
            |
            |private val `${function.name}-kpy-def` = `${function.name}-kpy-fun`.pydef("${function.exportName}", "${docstring.replace("\"", "\\\"")}", ${if (nargs == 0) "METH_NOARGS" else "METH_VARARGS or METH_KEYWORDS"})
            |
        """.trimMargin())
    }

    fun parseArgs(name: String, function: KSFunctionDeclaration) : String {
        val args = function.parameters.map { it.name!!.asString() }
        val nargs = args.size

        return """
            |val names = allocArrayOf(
            |            ${args.joinToString(", ") { "makeString(\"$it\")" } }, null
            |        )
            |        val params = List(${nargs}) { allocPointerTo<PyObject>() }
            |        PyArg_ParseTupleAndKeywords(args, kwargs, "${"O".repeat(nargs)}:$name", names, ${(0 until nargs).joinToString(", ") { "params[$it].ptr" } })
        """.trimMargin()
    }

    fun params(nullable: List<Boolean>) : String {
        return nullable.withIndex().joinToString(", ") {
            "params[${it.index}].value!!.pointed.ptr.toKotlin${if (it.value) "Nullable" else ""}()"
        }
    }
}
