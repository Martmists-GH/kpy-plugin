package kpy.internals

import kotlinx.cinterop.CPointer
import python.*

inline fun PyObject.PyObject_HEAD_INIT(type: CPointer<PyTypeObject>?) {
    ob_refcnt = 1
    ob_type = type
}

inline fun PyVarObject.PyVarObject_HEAD_INIT(type: CPointer<PyTypeObject>?, size: Py_ssize_t) {
    ob_base.PyObject_HEAD_INIT(type)
    ob_size = size
}

inline fun PyModuleDef_Base.PyModuleDef_HEAD_INIT() {
    ob_base.PyObject_HEAD_INIT(null)
    m_init = null
    m_index = 0
    m_copy = null
}
