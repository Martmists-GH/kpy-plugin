package kpy.ext

import kpy.wrappers.PyObjectT
import python.Py_DecRef
import python.Py_IncRef

fun PyObjectT.incref() : PyObjectT {
    this?.let(::Py_IncRef)
    return this
}

fun PyObjectT.decref() : PyObjectT {
    this?.let(::Py_DecRef)
    return this
}
