package kpy.wrappers

import kotlinx.cinterop.CFunction
import kotlinx.cinterop.CPointer
import python.PyObject


typealias FuncPtr<T> = CPointer<CFunction<T>>
typealias PyObjectT = CPointer<PyObject>?
typealias PyMethodT = (self: PyObjectT, args: PyObjectT) -> PyObjectT
typealias PyMethodKwargsT = (self: PyObjectT, args: PyObjectT, kwargs: PyObjectT) -> PyObjectT
