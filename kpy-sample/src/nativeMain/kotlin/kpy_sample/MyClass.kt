package kpy_sample

import kpy.annotations.PyExport

@PyExport
class MyClass {
    @PyExport
    fun test() {
        println("Hello, world!")
    }

    fun hidden() {
        println("This is hidden!")
    }
}

@PyExport
fun testFunction(x: Int) : Int {
    return x shr 1
}
