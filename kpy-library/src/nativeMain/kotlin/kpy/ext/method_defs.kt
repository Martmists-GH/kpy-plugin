package kpy.ext

import kotlinx.cinterop.CValue
import kotlinx.cinterop.cValue
import kpy.builders.makeString
import kpy.wrappers.FuncPtr
import kpy.wrappers.PyMethodKwargsT
import kpy.wrappers.PyMethodT
import python.*

inline fun FuncPtr<PyMethodT>.pydef(
    name: String,
    doc: String,
    flags: Int = METH_NOARGS
): CValue<PyMethodDef> {
    return cValue {
        ml_name = makeString(name)
        ml_doc = makeString(doc)
        ml_flags = flags
        ml_meth = this@pydef
    }
}

inline fun FuncPtr<PyMethodKwargsT>.pydef(
    name: String,
    doc: String,
    flags: Int = METH_VARARGS or METH_KEYWORDS
): CValue<PyMethodDef> {
    return cValue {
        ml_name = makeString(name)
        ml_doc = makeString(doc)
        ml_flags = flags
        ml_meth = this@pydef as FuncPtr<PyMethodT>
    }
}
