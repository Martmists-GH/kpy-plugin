package kpy_sample

import kotlinx.cinterop.toKString
import kpy.annotations.PyExport
import kpy.annotations.PyModuleHook
import kpy.wrappers.PyObjectT
import python.PyModule_GetName

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

@PyModuleHook
fun hook(obj: PyObjectT) {
    val name = PyModule_GetName(obj)!!.toKString()
    println("Creating module '$name'")
}
