package kpy_sample

import kotlinx.cinterop.toKString
import kpy.annotations.PyExport
import kpy.annotations.PyModuleHook
import kpy.wrappers.PyObjectT
import python.PyModule_GetName
import python.PyObject_Str

@PyExport
class MyClass {
    @PyExport
    fun test() {
        println("Hello, world!")
    }

    @PyExport
    fun testList(list: List<Int>) {
        println("List: $list")
    }

    @PyExport
    fun testMap(map: Map<String, Int>) {
        println("Map: $map")
    }

    @PyExport
    fun testObject(obj: PyObjectT) {
        println("Object: ${PyObject_Str(obj)}")
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
