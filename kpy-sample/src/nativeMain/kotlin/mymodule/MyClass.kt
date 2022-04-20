package mymodule

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
