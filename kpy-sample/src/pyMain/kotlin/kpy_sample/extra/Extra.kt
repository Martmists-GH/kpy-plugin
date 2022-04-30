package kpy_sample.extra

import kpy.annotations.PyExport

@PyExport
class Extra(private val x: Int) {
    @PyExport("x")
    fun getX(): Int {
        return x
    }

    @PyExport
    fun add(other: Extra?) : Extra {
        return Extra(x + (other?.getX() ?: 0))
    }
}
