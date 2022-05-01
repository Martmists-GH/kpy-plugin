package kpy_sample.extra

import kpy.annotations.PyExport
import kpy.annotations.PyMagic
import kpy.annotations.PyMagicMethod
import kpy.utilities.toPython
import kpy.wrappers.PyObjectT
import python.PyObject_GenericGetAttr

@PyExport
class More(x: Int) : Extra(x) {
    @PyExport
    fun test() {
        println("test")
    }

    @PyMagic(PyMagicMethod.TP_GETATTRO)
    internal fun __getattr__(name: String): PyObjectT {
        return PyObject_GenericGetAttr(this.toPython(), name.toPython())
    }
}
