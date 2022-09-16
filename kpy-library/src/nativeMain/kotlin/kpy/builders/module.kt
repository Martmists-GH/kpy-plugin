package kpy.builders

import kotlinx.cinterop.CValue
import kotlinx.cinterop.alloc
import kotlinx.cinterop.nativeHeap
import kpy.internals.PyModuleDef_HEAD_INIT
import python.*

fun makeModule(
    km_name: String,
    km_methods: List<CValue<PyMethodDef>>? = null,
    km_doc: String? = null,
    km_size: Py_ssize_t = 0L,
    km_free: freefunc? = null,
    km_clear: inquiry? = null,
    km_traverse: traverseproc? = null,
): PyModuleDef = nativeHeap.alloc<PyModuleDef> {
    m_base.PyModuleDef_HEAD_INIT()
    m_name = makeString(km_name)
    m_methods = km_methods?.let {
        makeArray(*it.toTypedArray())
    }
    m_doc = km_doc?.let(::makeString)
    m_size = km_size
    m_free = km_free
    m_clear = km_clear
    m_traverse = km_traverse
}
