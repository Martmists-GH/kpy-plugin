package kpy_sample.extra

import kpy.annotations.PyExport

@PyExport
class More(x: Int) : Extra(x) {
    @PyExport
    fun test() {
        println("test")
    }
}
