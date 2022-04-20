package kpy.utilities

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.convert
import kotlinx.cinterop.toKStringFromUtf8
import kpy.wrappers.PyObjectT
import python.*
import kotlin.reflect.KType
import kotlin.reflect.typeOf

inline fun <reified R : Any> PyObjectT.toKotlin() : R = toKotlin(typeOf<R>())

@Suppress("IMPLICIT_CAST_TO_ANY")
fun <R : Any> PyObjectT.toKotlin(type: KType?) : R {
    val clazz = type?.classifier

    return when (clazz) {
        Int::class -> PyLong_AsLong(this).toInt()
        Long::class -> PyLong_AsLong(this)
        Float::class -> PyFloat_AsDouble(this).toFloat()
        Double::class -> PyFloat_AsDouble(this)
        String::class -> PyUnicode_AsString(this)!!.toKStringFromUtf8()
        Boolean::class -> PyObject_IsTrue(this) == 1
        FloatArray::class -> {
            val size = PyList_Size(this)
            FloatArray(size.convert()) { i ->
                PyList_GetItem(this, i.convert()).toKotlin()
            }
        }
        IntArray::class -> {
            val size = PyList_Size(this)
            IntArray(size.convert()) { i ->
                PyList_GetItem(this, i.convert()).toKotlin()
            }
        }
        List::class -> {
            val subType = type.arguments[0].type
            val size = PyList_Size(this)
            List(size.convert()) { i ->
                PyList_GetItem(this, i.convert()).toKotlin(subType) as Any
            }
        }
        Map::class, MutableMap::class -> {
            val keyType = type.arguments[0].type
            val valueType = type.arguments[1].type
            val size = PyDict_Size(this)

            val keys = PyDict_Keys(this).toKotlin<List<PyObjectT>>()
            val map = mutableMapOf<Any, Any>()


            for (i in 0 until size) {
                val key = keys[i.convert()]
                val pKey = key.toKotlin(keyType) as Any
                val pValue = PyDict_GetItem(this, key).toKotlin(valueType) as Any
                map[pKey] = pValue
            }
        }
        Pair::class -> {
            val firstType = type.arguments[0].type
            val secondType = type.arguments[1].type

            val first = PyTuple_GetItem(this, 0).toKotlin(firstType) as Any
            val second = PyTuple_GetItem(this, 1).toKotlin(secondType) as Any
            Pair(first, second)
        }
        Triple::class -> {
            val firstType = type.arguments[0].type
            val secondType = type.arguments[1].type
            val thirdType = type.arguments[2].type

            val first = PyTuple_GetItem(this, 0).toKotlin(firstType) as Any
            val second = PyTuple_GetItem(this, 1).toKotlin(secondType) as Any
            val third = PyTuple_GetItem(this, 2).toKotlin(thirdType) as Any
            Triple(first, second, third)
        }
        CPointer::class, CValuesRef::class, null -> {
            // Assume PyObjectT
            this
        }
        else -> throw IllegalArgumentException("Unsupported type: $clazz")
    } as R
}

fun <T> T.toPython() : PyObjectT {
    return when (this) {
        null -> Py_None
        is Unit -> Py_None
        is Int -> PyLong_FromLong(this.toLong())
        is Long -> PyLong_FromLong(this)
        is Float -> PyFloat_FromDouble(this.toDouble())
        is Double -> PyFloat_FromDouble(this)
        is String -> PyUnicode_FromString(this)
        is Boolean -> PyBool_FromLong(if (this) 1 else 0)
        is FloatArray -> {
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
        else -> throw IllegalArgumentException("Unsupported type: ${this!!::class.simpleName}")
    }
}
