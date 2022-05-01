package kpy_sample.extra

import kpy.annotations.PyExport
import kpy.annotations.PyMagic
import kpy.annotations.PyMagicMethod
import kpy.annotations.PyMagicMethod.NB_INVERT

@PyExport
class Extra(private val x: Int) {
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
}
