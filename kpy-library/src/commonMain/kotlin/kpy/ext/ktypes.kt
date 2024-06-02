package kpy.ext

import kotlinx.cinterop.*
import kpy.wrappers.PyObjectT
import python.*

val CPointer<PyObject>?.kt
    get(): CPointer<KtPyObject> = this!!.reinterpret()

inline fun <reified T : Any> CPointer<KtPyObject>.cast(): T {
    val ref = this.pointed.ktObject!!.asStableRef<T>()
    return ref.get()
}

inline fun PyObjectT.addType(type: PyTypeObject): Int {
    var status = PyType_Ready(type.ptr)
    if (status < 0) {
        PyErr_Print()
    } else {
        status = PyModule_AddType(this@addType, type.ptr)
    }
    return status
}
