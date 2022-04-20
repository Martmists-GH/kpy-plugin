package kpy.wrappers

import kotlinx.cinterop.*
import kpy.utilities.Freeable
import python.*


val PyType_GenericAllocKt = staticCFunction { arg0: CPointer<PyTypeObject>?, arg1: Py_ssize_t ->
    return@staticCFunction PyType_GenericAlloc(arg0, arg1)
}

val PyType_GenericNewKt = staticCFunction { arg0: CPointer<PyTypeObject>?, arg1: CPointer<PyObject>?, arg2: CPointer<PyObject>? ->
    return@staticCFunction PyType_GenericNew(arg0, arg1, arg1)
}


// Default __del__ implementation
val KtType_StableRefFree = staticCFunction { self: COpaquePointer? ->
    val selfObj: CPointer<KtPyObject> = self?.reinterpret() ?: return@staticCFunction
    val ref = selfObj.pointed.ktObject?.asStableRef<Any>()

    val obj = ref?.get()
    if (obj is Freeable) {
        obj.free()
    }

    ref?.dispose()
}

// Default __repr__ implementation
val KtType_StableRefRepr = staticCFunction { self: PyObjectT ->
    val selfObj: CPointer<KtPyObject> = self?.reinterpret() ?: return@staticCFunction null
    val ref = selfObj.pointed.ktObject!!.asStableRef<Any>()
    return@staticCFunction PyUnicode_FromString(ref.get().toString())
}
