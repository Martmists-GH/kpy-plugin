package kpy_sample.extra

import kpy.annotations.PyExport

@PyExport
class Extra(private val x: Int) {
    @PyExport("x")
    fun getX(): Int {
        return x
    }
}
