package com.martmists.kpy.processor.generation

import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.martmists.kpy.processor.collected.KPyClass
import com.martmists.kpy.processor.collected.KPyFunction
import com.martmists.kpy.processor.collected.KPyModule
import com.martmists.kpy.processor.ext.toSnakeCase
import java.io.OutputStreamWriter

class KPyCodeGenerator(private val projectName: String, private val generateStubs: Boolean) {
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
            |    ${module.properties.joinToString("\n    ") { "val `${it.name}-kpy` = ${it.name}.toPython(); if (PyModule_AddObject(obj, \"${it.exportName}\", `${it.name}-kpy`) < 0) { `${it.name}-kpy`.decref(); return null }" }}
            |    ${module.hooks.joinToString("\n    ") { "${it.simpleName.asString()}(obj)" }}
            |    ${module.classes.joinToString("\n    ") { "if (obj.addType(KPyType_${it.exportName}) < 0) return null" }}
            |    ${module.children.joinToString("\n    ") { "if (PyModule_AddObject(obj, \"${it.key}\", ${it.name}.createModule().incref()) < 0) return null" }}
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
            |PyMODINIT_FUNC PyInit_${if (generateStubs) "_${module.name}" else module.name} (void) {
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

        for (magic in clazz.magic) {
            generateMagic(clazz, magic)
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
            |try {
            |        memScoped {
            |            val selfObj: CPointer<KtPyObject> = self?.reinterpret() ?: return@memScoped -1
            |            ${parseArgs("__init__", constructor)}
            |    
            |            val instance = ${clazz.name}(
            |                ${params(constructor.parameters.map { it.type.resolve().isMarkedNullable })}
            |            )
            |            val ref = StableRef.create(instance)
            |            selfObj.pointed.ktObject = ref.asCPointer()
            |            self.remember(instance)
            |            0
            |        }
            |    } catch (e: Throwable) {
            |        PyErr_SetString(PyExc_Exception, e.toString())
            |        -1
            |    }
            |
        """.trimMargin() else """
            |try {
            |        memScoped {
            |            val selfObj: CPointer<KtPyObject> = self?.reinterpret() ?: return@memScoped -1
            |            val instance = ${clazz.name}()
            |            val ref = StableRef.create(instance)
            |            selfObj.pointed.ktObject = ref.asCPointer()
            |            self.remember(instance)
            |            0
            |        }
            |    } catch (e: Throwable) {
            |        PyErr_SetString(PyExc_Exception, e.toString())
            |        -1
            |    }
            |
        """.trimMargin()

        val parentClass = if (clazz.parent == null) "PyBaseObject_Type" else "`${clazz.parent.declaration.packageName.asString()}`.`KPyType_${clazz.parent.name}`"

        fun magicAttr(name: String) : String {
            val func = clazz.magic.find { it.exportName == name } ?: return ""
            return "k$name=`${clazz.name}-${func.name}-kpy-magic-fun`,"
        }

        fun magicFields(key: String, vararg names: String) : String {
            val exportNames = clazz.magic.map { it.exportName }
            if (exportNames.any { it in names }) {
                return names.filter { it in exportNames }.joinToString(", ", "k$key=mapOf(", "),") { s ->
                    val func = clazz.magic.first { it.exportName == s }
                    val name = "`${clazz.name}-${func.name}-kpy-magic-fun`"
                    "\"$s\" to $name"
                }
            }
            return ""
        }

        val magic_props = """
            |${magicAttr("tp_getattro")}
            |${magicAttr("tp_setattro")}
            |${magicAttr("tp_richcompare")}
            |${magicAttr("tp_iter")}
            |${magicAttr("tp_iternext")}
            |${magicAttr("tp_traverse")}
            |${magicFields("tp_as_async", "am_await", "am_aiter", "am_anext")}
            |${magicFields("tp_as_number", 
                "nb_absolute", "nb_add", "nb_and", 
                "nb_bool", "nb_divmod", "nb_float", "nb_floor_divide", "nb_index", 
                "nb_inplace_add", "nb_inplace_and", "nb_inplace_floor_divide", 
                "nb_inplace_lshift", "nb_inplace_matrix_multiply", 
                "nb_inplace_multiply", "nb_inplace_or", "nb_inplace_power", 
                "nb_inplace_remainder", "nb_inplace_rshift", "nb_inplace_subtract", 
                "nb_inplace_true_divide", "nb_inplace_xor", "nb_int", "nb_invert", 
                "nb_lshift", "nb_matrix_multiply", "nb_multiply", "nb_negative", 
                "nb_or", "nb_positive", "nb_power", "nb_remainder", "nb_rshift", 
                "nb_subtract", "nb_true_divide", "nb_xor")}
            |${magicFields("tp_as_sequence", "sq_length", "sq_concat", "sq_repeat", "sq_item", "sq_ass_item", "sq_contains", "sq_inplace_concat", "sq_inplace_repeat", "was_sq_slice", "was_sq_ass_slice")}
            |${magicFields("tp_as_mapping", "mp_length", "mp_subscript", "mp_ass_subscript")}
            |${magicFields("tp_as_buffer", "bf_getbuffer", "bf_releasebuffer")}
        """.trimMargin().replace(Regex("\n\n+"), "\n")

        write("""
            |private val `${clazz.name}-kpy-init` = staticCFunction { self: PyObjectT, args: PyObjectT, kwargs: PyObjectT ->
            |    ${if (clazz.declaration.isAbstract()) abstractBody else implBody}
            |}
            |
            |val KPyType_${clazz.name} = makePyType(
            |    name = "${clazz.declaration.packageName.asString()}.${clazz.exportName}",
            |    ktp_init = `${clazz.name}-kpy-init`,
            |    ktp_base = ${parentClass}.ptr,
            |    ktp_methods = listOf(
            |        ${clazz.functions.joinToString(",\n") { "`${clazz.name}-${it.name}-kpy-def`" }}                
            |    ),
            |    $magic_props
            |    ${if (clazz.declaration.annotations.firstOrNull { it.shortName.getShortName() == "PyDictClass" } != null) "ktp_has_dictoffset = true," else ""}
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
            |            val result = selfKt.${function.name}(
            |                ${params(params.map { it.type.resolve().isMarkedNullable })}
            |            )
        """.trimMargin() else """
            |val result = selfKt.${function.name}()
        """.trimMargin()

        write("""
            |private val `${clazz.name}-${function.name}-kpy-fun` = staticCFunction { self: PyObjectT, args: PyObjectT, kwargs: PyObjectT ->
            |    try {
            |        memScoped {
            |            val selfKt = self!!.kt.cast<${clazz.name}>()
            |            $body
            |            result.toPython()
            |        }
            |    } catch (e: Throwable) {
            |        PyErr_SetString(PyExc_Exception, e.toString())
            |        null
            |    }
            |}
            |
            |private val `${clazz.name}-${function.name}-kpy-def` = `${clazz.name}-${function.name}-kpy-fun`.pydef(
            |    "${function.exportName}", "${docstring.replace("\"", "\\\"").replace("\n", "\\n")}", ${if (nargs == 0) "METH_NOARGS" else "METH_VARARGS or METH_KEYWORDS"}
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
            |    try {
            |        ${if (nargs == 0) bodyNoParams else bodyParams}
            |    } catch (e: Throwable) {
            |        PyErr_SetString(PyExc_Exception, e.toString())
            |        null
            |    } 
            |}
            |
            |private val `${function.name}-kpy-def` = `${function.name}-kpy-fun`.pydef("${function.exportName}", "${docstring.replace("\"", "\\\"").replace("\n", "\\n")}", ${if (nargs == 0) "METH_NOARGS" else "METH_VARARGS or METH_KEYWORDS"})
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

    context(OutputStreamWriter)
    fun generateMagic(clazz: KPyClass, function: KPyFunction) {
        when (function.exportName) {
            "tp_iter", "tp_iternext", "am_await", "am_aiter", "am_anext", "nb_negative", "nb_positive", "nb_absolute", "nb_invert", "nb_int", "nb_float", "nb_index" -> generateUnaryfunc(clazz, function)
            "tp_getattro", "nb_add", "nb_inplace_add", "nb_subtract", "nb_inplace_subtract", "nb_multiply", "nb_inplace_multiply", "nb_remainder",
                "nb_inplace_remainder", "nb_divmod", "nb_lshift", "nb_inplace_lshift", "nb_rshift", "nb_inplace_rshift", "nb_and", "nb_inplace_and",
                "nb_xor", "nb_inplace_xor", "nb_or", "nb_inplace_or", "nb_floor_divide", "nb_inplace_floor_divide", "nb_true_divide", "nb_inplace_true_divide",
                "nb_matrix_multiply", "nb_inplace_matrix_multiply", "mp_subscript", "sq_concat", "sq_inplace_concat", "sq_inplace_repeat" -> generateBinaryfunc(clazz, function)
            "nb_power", "nb_inplace_power" -> generateTernaryfunc(clazz, function)
            "tp_traverse" -> generateTraverseproc(clazz, function)
            "tp_richcompare" -> generateRichcmpfunc(clazz, function)
            "nb_bool" -> generateInquiry(clazz, function)
            "mp_length", "sq_length" -> generateLenfunc(clazz, function)
            "tp_setattro", "mp_ass_subscript" -> generateObjobjargproc(clazz, function)
            "sq_repeat", "sq_item" -> generateSsizeargfunc(clazz, function)
            "sq_ass_item" -> generateSsizeobjargproc(clazz, function)
            "sq_contains" -> generateObjobjproc(clazz, function)
            "bf_getbuffer" -> generateGetbufferproc(clazz, function)
            "bf_releasebuffer" -> generateReleasebufferproc(clazz, function)
        }
    }

    context(OutputStreamWriter)
    fun generateUnaryfunc(clazz: KPyClass, function: KPyFunction) {
        write("""
            |private val `${clazz.name}-${function.name}-kpy-magic-fun` = staticCFunction { self: PyObjectT ->
            |    try {
            |        memScoped {
            |            val selfKt = self!!.kt.cast<${clazz.name}>()
            |            val result = selfKt.${function.name}()
            |            result.toPython()
            |        }
            |    } catch (e: Throwable) {
            |        PyErr_SetString(PyExc_Exception, e.toString())
            |        null
            |    }
            |}
            |
        """.trimMargin())
    }

    context(OutputStreamWriter)
    fun generateBinaryfunc(clazz: KPyClass, function: KPyFunction) {
        write("""
            |private val `${clazz.name}-${function.name}-kpy-magic-fun` = staticCFunction { self: PyObjectT, arg: PyObjectT ->
            |    try {
            |        memScoped {
            |            val selfKt = self!!.kt.cast<${clazz.name}>()
            |            val result = selfKt.${function.name}(arg.toKotlin())
            |            result.toPython()
            |        }
            |    } catch (e: Throwable) {
            |        PyErr_SetString(PyExc_Exception, e.toString())
            |        null
            |    }
            |}
            |
        """.trimMargin())
    }

    context(OutputStreamWriter)
    fun generateTernaryfunc(clazz: KPyClass, function: KPyFunction) {
        write("""
            |private val `${clazz.name}-${function.name}-kpy-magic-fun` = staticCFunction { self: PyObjectT, arg: PyObjectT, arg2: PyObjectT ->
            |    try {
            |        memScoped {
            |            val selfKt = self!!.kt.cast<${clazz.name}>()
            |            val result = selfKt.${function.name}(arg.toKotlin(), arg2.toKotlin())
            |            result.toPython()
            |        }
            |    } catch (e: Throwable) {
            |        PyErr_SetString(PyExc_Exception, e.toString())
            |        null
            |    }
            |}
            |
        """.trimMargin())
    }

    context(OutputStreamWriter)
    fun generateTraverseproc(clazz: KPyClass, function: KPyFunction) {
        write("""
            |private val `${clazz.name}-${function.name}-kpy-magic-fun` = staticCFunction { self: PyObjectT, arg: visitproc, arg2: COpaquePointer ->
            |    memScoped {
            |        val selfKt = self!!.kt.cast<${clazz.name}>()
            |        val result = selfKt.${function.name}(arg, arg2)
            |        result
            |    }
            |}
            |
        """.trimMargin())
    }

    context(OutputStreamWriter)
    fun generateRichcmpfunc(clazz: KPyClass, function: KPyFunction) {
        write("""
            |private val `${clazz.name}-${function.name}-kpy-magic-fun` = staticCFunction { self: PyObjectT, arg: PyObjectT, arg2: Int ->
            |    try {
            |        memScoped {
            |            val selfKt = self!!.kt.cast<${clazz.name}>()
            |            val result = selfKt.${function.name}(arg.toKotlin(), arg2)
            |            result.toPython()
            |        }
            |    } catch (e: Throwable) {
            |        PyErr_SetString(PyExc_Exception, e.toString())
            |        null
            |    }
            |}
            |
        """.trimMargin())
    }

    context(OutputStreamWriter)
    fun generateInquiry(clazz: KPyClass, function: KPyFunction) {
        write("""
            |private val `${clazz.name}-${function.name}-kpy-magic-fun` = staticCFunction { self: PyObjectT ->
            |    memScoped {
            |        val selfKt = self!!.kt.cast<${clazz.name}>()
            |        val result = selfKt.${function.name}()
            |        result
            |    }
            |}
            |
        """.trimMargin())
    }

    context(OutputStreamWriter)
    fun generateLenfunc(clazz: KPyClass, function: KPyFunction) {
        write("""
            |private val `${clazz.name}-${function.name}-kpy-magic-fun` = staticCFunction { self: PyObjectT ->
            |    try {
            |        memScoped {
            |            val selfKt = self!!.kt.cast<${clazz.name}>()
            |            val result = selfKt.${function.name}()
            |            result.convert<Py_ssize_t>()
            |        }
            |    } catch (e: Throwable) {
            |        PyErr_SetString(PyExc_Exception, e.toString())
            |        (-1).convert<Py_ssize_t>()
            |    }
            |}
            |
        """.trimMargin())
    }

    context(OutputStreamWriter)
    fun generateObjobjargproc(clazz: KPyClass, function: KPyFunction) {
        write("""
            |private val `${clazz.name}-${function.name}-kpy-magic-fun` = staticCFunction { self: PyObjectT, arg: PyObjectT, arg2: PyObjectT ->
            |    try {
            |        memScoped {
            |            val selfKt = self!!.kt.cast<${clazz.name}>()
            |            val result = selfKt.${function.name}(arg.toKotlin(), arg2.toKotlin())
            |            result
            |        }
            |    } catch (e: Throwable) {
            |        PyErr_SetString(PyExc_Exception, e.toString())
            |        -1
            |    }
            |}
            |
        """.trimMargin())
    }

    context(OutputStreamWriter)
    fun generateSsizeargfunc(clazz: KPyClass, function: KPyFunction) {
        write("""
            |private val `${clazz.name}-${function.name}-kpy-magic-fun` = staticCFunction { self: PyObjectT, arg: Py_ssize_t ->
            |    try {
            |        memScoped {
            |            val selfKt = self!!.kt.cast<${clazz.name}>()
            |            val result = selfKt.${function.name}(arg.convert())
            |            result.toPython()
            |        }
            |    } catch (e: Throwable) {
            |        PyErr_SetString(PyExc_Exception, e.toString())
            |        null
            |    }
            |}
            |
        """.trimMargin())
    }

    context(OutputStreamWriter)
    fun generateSsizeobjargproc(clazz: KPyClass, function: KPyFunction) {
        write("""
            |private val `${clazz.name}-${function.name}-kpy-magic-fun` = staticCFunction { self: PyObjectT, arg: Py_ssize_t ->
            |    try {
            |        memScoped {
            |            val selfKt = self!!.kt.cast<${clazz.name}>()
            |            val result = selfKt.${function.name}(arg.convert())
            |            result
            |        }
            |    } catch (e: Throwable) {
            |        PyErr_SetString(PyExc_Exception, e.toString())
            |        -1
            |    }
            |}
            |
        """.trimMargin())
    }

    context(OutputStreamWriter)
    fun generateObjobjproc(clazz: KPyClass, function: KPyFunction) {
        write("""
            |private val `${clazz.name}-${function.name}-kpy-magic-fun` = staticCFunction { self: PyObjectT, arg: PyObjectT ->
            |    try {
            |        memScoped {
            |            val selfKt = self!!.kt.cast<${clazz.name}>()
            |            val result = selfKt.${function.name}(arg.toKotlin())
            |            result
            |        }
            |    } catch (e: Throwable) {
            |        PyErr_SetString(PyExc_Exception, e.toString())
            |        -1
            |    }
            |}
            |
        """.trimMargin())
    }

    context(OutputStreamWriter)
    fun generateGetbufferproc(clazz: KPyClass, function: KPyFunction) {
        write("""
            |private val `${clazz.name}-${function.name}-kpy-magic-fun` = staticCFunction { self: PyObjectT, view: Py_buffer, flags: Int ->
            |    memScoped {
            |        val selfKt = self!!.kt.cast<${clazz.name}>()
            |        val result = selfKt.${function.name}(view.toKotlin(), flags)
            |        result
            |    }
            |}
            |
        """.trimMargin())
    }

    context(OutputStreamWriter)
    fun generateReleasebufferproc(clazz: KPyClass, function: KPyFunction) {
        write("""
            |private val `${clazz.name}-${function.name}-kpy-magic-fun` = staticCFunction { self: PyObjectT, view: Py_buffer ->
            |    memScoped {
            |        val selfKt = self!!.kt.cast<${clazz.name}>()
            |        selfKt.${function.name}(view.toKotlin())
            |    }
            |}
            |
        """.trimMargin())
    }
}
