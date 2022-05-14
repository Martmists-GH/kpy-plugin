package kpy_sample.extra

import kpy.annotations.PyHint
import kpy.annotations.PyExport
import kpy.annotations.PyMagic
import kpy.annotations.PyMagicMethod
import kpy.annotations.PyMagicMethod.NB_INVERT
import kpy.utilities.toPython
import kpy.wrappers.PyObjectT
import python.PyObject_GenericGetAttr

@PyExport
open class Extra(private val x: Int) {
    @PyHint
    val someProp: FloatArray? = null

    /**
     * return the value of x
     */
    @PyExport("x")
    fun getX(): Int {
        return x
    }

    @PyMagic(PyMagicMethod.NB_ADD)
    fun add(other: Extra?) : Extra {
        return Extra(x + (other?.getX() ?: 0))
    }

    @PyMagic(NB_INVERT)
    fun invert() {

    }

    @PyMagic(PyMagicMethod.TP_GETATTRO)
    internal fun __getattr__(name: String): PyObjectT {
        return PyObject_GenericGetAttr(this.toPython(), name.toPython())
    }
}
