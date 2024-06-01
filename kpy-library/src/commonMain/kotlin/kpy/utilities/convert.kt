package kpy.utilities

import kotlinx.cinterop.*
import kpy.ext.cast
import kpy.ext.incref
import kpy.ext.kt
import kpy.wrappers.PyObjectT
import platform.posix.memcpy
import python.*
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@PublishedApi
internal val typeMap = mutableMapOf<KType, PyTypeObject>()
internal val instanceMap = mutableMapOf<Any, PyObjectT>()

// Avoid using these
inline fun <reified K> PyTypeObject._registerType() : PyTypeObject {
    typeMap[typeOf<K>()] = this
    return this
}
fun PyObjectT.remember(item: Any) {
    instanceMap[item] = this
}
fun Any.forget() {
    instanceMap.remove(this)
}

inline fun <reified R : Any> PyObjectT.toKotlin() : R = toKotlin(typeOf<R>())
inline fun <reified R : Any> PyObjectT.toKotlinNullable() : R? = toKotlinNullable(typeOf<R>())

@Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
fun <R : Any> PyObjectT.toKotlin(type: KType?) : R {
    val clazz = type?.classifier

    @Suppress("RemoveRedundantCallsOfConversionMethods")
    return when (clazz) {
        Int::class -> {
            require(PyObject_IsInstance(this, PyLong_Type.ob_base.ob_base.ptr) == 1) { "Expected int, got ${(PyObject_Type(this) as CPointer<PyTypeObject>?)!!.pointed.tp_name!!.toKString()}" }
            PyLong_AsLong(this).toInt()
        }
        Long::class -> {
            require(PyObject_IsInstance(this, PyLong_Type.ob_base.ob_base.ptr) == 1) { "Expected int, got ${(PyObject_Type(this) as CPointer<PyTypeObject>?)!!.pointed.tp_name!!.toKString()}" }
            PyLong_AsLong(this).toLong()
        }
        Float::class -> {
            require(PyObject_IsInstance(this, PyFloat_Type.ob_base.ob_base.ptr) == 1) { "Expected float, got ${(PyObject_Type(this) as CPointer<PyTypeObject>?)!!.pointed.tp_name!!.toKString()}" }
            PyFloat_AsDouble(this).toFloat()
        }
        Double::class -> {
            require(PyObject_IsInstance(this, PyFloat_Type.ob_base.ob_base.ptr) == 1) { "Expected float, got ${(PyObject_Type(this) as CPointer<PyTypeObject>?)!!.pointed.tp_name!!.toKString()}" }
            PyFloat_AsDouble(this)
        }
        String::class -> {
            require(PyObject_IsInstance(this, PyUnicode_Type.ob_base.ob_base.ptr) == 1) { "Expected str, got ${(PyObject_Type(this) as CPointer<PyTypeObject>?)!!.pointed.tp_name!!.toKString()}" }
            PyUnicode_AsString(this)!!.toKStringFromUtf8()
        }
        Boolean::class -> {
            require(PyObject_IsInstance(this, PyBool_Type.ob_base.ob_base.ptr) == 1) { "Expected bool, got ${(PyObject_Type(this) as CPointer<PyTypeObject>?)!!.pointed.tp_name!!.toKString()}" }
            PyObject_IsTrue(this) == 1
        }
        FloatArray::class -> {
            require(PyObject_IsInstance(this, PyList_Type.ob_base.ob_base.ptr) == 1) { "Expected list, got ${(PyObject_Type(this) as CPointer<PyTypeObject>?)!!.pointed.tp_name!!.toKString()}" }
            val size = PyList_Size(this)
            FloatArray(size.convert()) { i ->
                PyList_GetItem(this, i.convert()).toKotlin()
            }
        }
        DoubleArray::class -> {
            require(PyObject_IsInstance(this, PyList_Type.ob_base.ob_base.ptr) == 1) { "Expected list, got ${(PyObject_Type(this) as CPointer<PyTypeObject>?)!!.pointed.tp_name!!.toKString()}" }
            val size = PyList_Size(this)
            DoubleArray(size.convert()) { i ->
                PyList_GetItem(this, i.convert()).toKotlin()
            }
        }
        IntArray::class -> {
            require(PyObject_IsInstance(this, PyList_Type.ob_base.ob_base.ptr) == 1) { "Expected list, got ${(PyObject_Type(this) as CPointer<PyTypeObject>?)!!.pointed.tp_name!!.toKString()}" }
            val size = PyList_Size(this)
            IntArray(size.convert()) { i ->
                PyList_GetItem(this, i.convert()).toKotlin()
            }
        }
        LongArray::class -> {
            require(PyObject_IsInstance(this, PyList_Type.ob_base.ob_base.ptr) == 1) { "Expected list, got ${(PyObject_Type(this) as CPointer<PyTypeObject>?)!!.pointed.tp_name!!.toKString()}" }
            val size = PyList_Size(this)
            LongArray(size.convert()) { i ->
                PyList_GetItem(this, i.convert()).toKotlin()
            }
        }
        ByteArray::class -> {
            require(PyObject_IsInstance(this, PyBytes_Type.ob_base.ob_base.ptr) == 1) { "Expected bytes, got ${(PyObject_Type(this) as CPointer<PyTypeObject>?)!!.pointed.tp_name!!.toKString()}" }
            val size = PyBytes_Size(this)
            PyBytes_AsString(this)!!.readBytes(size.convert())
        }
        List::class -> {
            require(PyObject_IsInstance(this, PyList_Type.ob_base.ob_base.ptr) == 1) { "Expected list, got ${(PyObject_Type(this) as CPointer<PyTypeObject>?)!!.pointed.tp_name!!.toKString()}" }

            val subType = type.arguments[0].type
            val size = PyList_Size(this)
            if (subType?.isMarkedNullable == true) {
                List(size.convert()) { i ->
                    PyList_GetItem(this, i.convert()).toKotlinNullable(subType) as? Any
                }
            } else {
                List(size.convert()) { i ->
                    PyList_GetItem(this, i.convert()).toKotlin(subType) as Any
                }
            }
        }
        Map::class, MutableMap::class -> {
            require(PyObject_IsInstance(this, PyDict_Type.ob_base.ob_base.ptr) == 1) { "Expected dict, got ${(PyObject_Type(this) as CPointer<PyTypeObject>?)!!.pointed.tp_name!!.toKString()}" }
            val keyType = type.arguments[0].type
            val valueType = type.arguments[1].type
            val size = PyDict_Size(this)

            val keys = PyDict_Keys(this).toKotlin<List<PyObjectT>>()
            val map = mutableMapOf<Any, Any?>()

            for (i in 0 until size) {
                val key = keys[i.convert()]
                val pKey = key.toKotlin(keyType) as Any
                val pValue = if (valueType?.isMarkedNullable == true) {
                    PyDict_GetItem(this, key).toKotlinNullable(valueType) as Any?
                } else {
                    PyDict_GetItem(this, key).toKotlin(valueType) as Any
                }
                map[pKey] = pValue
            }

            map
        }
        Pair::class -> {
            val firstType = type.arguments[0].type
            val secondType = type.arguments[1].type

            val first = if (firstType?.isMarkedNullable == true) {
                PyTuple_GetItem(this, 0).toKotlinNullable(firstType) as? Any
            } else {
                PyTuple_GetItem(this, 0).toKotlin(firstType) as Any
            }
            val second = if (secondType?.isMarkedNullable == true) {
                PyTuple_GetItem(this, 1).toKotlinNullable(secondType) as? Any
            } else {
                PyTuple_GetItem(this, 1).toKotlin(secondType) as Any
            }
            Pair(first, second)
        }
        Triple::class -> {
            val firstType = type.arguments[0].type
            val secondType = type.arguments[1].type
            val thirdType = type.arguments[2].type

            val first = if (firstType?.isMarkedNullable == true) {
                PyTuple_GetItem(this, 0).toKotlinNullable(firstType) as? Any
            } else {
                PyTuple_GetItem(this, 0).toKotlin(firstType) as Any
            }
            val second = if (secondType?.isMarkedNullable == true) {
                PyTuple_GetItem(this, 1).toKotlinNullable(secondType) as? Any
            } else {
                PyTuple_GetItem(this, 1).toKotlin(secondType) as Any
            }
            val third = if (thirdType?.isMarkedNullable == true) {
                PyTuple_GetItem(this, 2).toKotlinNullable(thirdType) as? Any
            } else {
                PyTuple_GetItem(this, 2).toKotlin(thirdType) as Any
            }
            Triple(first, second, third)
        }
        CPointer::class, CValuesRef::class, null -> {
            // Assume PyObjectT
            this
        }
        else -> {
            // Assume usertype
            this.kt.cast()
        }
    } as R
}

@Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
fun <R : Any> PyObjectT.toKotlinNullable(type: KType?) : R? {
    return if (type?.isMarkedNullable == true && this == Py_None) null else toKotlin(type)
}

inline fun <reified T> T.toPython() = toPython(typeOf<T>())

@Suppress("UNNECESSARY_NOT_NULL_ASSERTION")  // False positive
fun <T> T.toPython(type: KType) : PyObjectT {
    return when (this) {
        null, is Unit -> Py_None.incref()
        is Int -> PyLong_FromLong(this.convert())
        is Long -> PyLong_FromLong(this.convert())
        is Float -> PyFloat_FromDouble(this.toDouble())
        is Double -> PyFloat_FromDouble(this)
        is String -> PyUnicode_FromString(this)
        is Boolean -> PyBool_FromLong((if (this) 1 else 0).convert())
        is FloatArray -> {
            val list = PyList_New(this.size.convert())
            for (i in 0 until this.size) {
                PyList_SetItem(list, i.convert(), this[i].toPython())
            }
            list
        }
        is DoubleArray -> {
            val list = PyList_New(this.size.convert())
            for (i in 0 until this.size) {
                PyList_SetItem(list, i.convert(), this[i].toPython())
            }
            list
        }
        is IntArray -> {
            val list = PyList_New(this.size.convert())
            for (i in 0 until this.size) {
                PyList_SetItem(list, i.convert(), this[i].toPython())
            }
            list
        }
        is LongArray -> {
            val list = PyList_New(this.size.convert())
            for (i in 0 until this.size) {
                PyList_SetItem(list, i.convert(), this[i].toPython())
            }
            list
        }
        is ByteArray -> {
            memScoped {
                val arr = allocArray<ByteVar>(this@toPython.size.convert())
                val bytes = PyBytes_FromStringAndSize(null, this@toPython.size.convert())
                memcpy(PyBytes_AsString(bytes)!!, arr, (sizeOf<ByteVar>() * this@toPython.size).convert())
                bytes
            }
        }
        is List<*> -> {
            val list = PyList_New(this.size.convert())
            for (i in 0 until this.size) {
                PyList_SetItem(list, i.convert(), this[i].toPython())
            }
            list
        }
        is Map<*, *> -> {
            val dict = PyDict_New()
            for ((k, v) in this) {
                PyDict_SetItem(dict, k.toPython(), v.toPython())
            }
            dict
        }
        is Pair<*, *> -> {
            val tuple = PyTuple_New(2)
            PyTuple_SetItem(tuple, 0, this.first.toPython())
            PyTuple_SetItem(tuple, 1, this.second.toPython())
            tuple
        }
        is Triple<*, *, *> -> {
            val tuple = PyTuple_New(3)
            PyTuple_SetItem(tuple, 0, this.first.toPython())
            PyTuple_SetItem(tuple, 1, this.second.toPython())
            PyTuple_SetItem(tuple, 2, this.third.toPython())
            tuple
        }
        is CPointer<*> -> {
            this as PyObjectT
        }
        else -> {
            // Assume usertype
            instanceMap[this] ?: let {
                // Create new
                PyType_GenericNew(typeMap[type]!!.ptr, null, null).also {
                    val ref = StableRef.create(this)
                    it.kt.pointed.ktObject = ref.asCPointer()
                    it.remember(this)
                }
            }
        }
    }
}
