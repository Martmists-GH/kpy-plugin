package kpy.builders

import kotlinx.cinterop.*
import kotlinx.cinterop.alloc
import kpy.internals.PyVarObject_HEAD_INIT
import kpy.wrappers.*
import kpy.wrappers.KtType_StableRefFree
import kpy.wrappers.PyType_GenericAllocKt
import kpy.wrappers.PyType_GenericNewKt
import python.*

val Py_TPFLAGS_DEFAULT = Py_TPFLAGS_HAVE_STACKLESS_EXTENSION.toULong() or Py_TPFLAGS_HAVE_VERSION_TAG.toULong()

inline fun <reified T> makePyType(
    ktp_dealloc: destructor? = null,
    ktp_vectorcall_offset: Py_ssize_t = 0,
    ktp_as_async: Map<String, unaryfunc>? = null,
    ktp_repr: reprfunc? = KtType_StableRefRepr,
    ktp_as_number: Map<String, CPointer<out CFunction<*>>>? = null,
    ktp_as_sequence: Map<String, CPointer<out CFunction<*>>>? = null,
    ktp_as_mapping: Map<String, CPointer<out CFunction<*>>>? = null,
    ktp_hash: hashfunc? = null,
    ktp_call: ternaryfunc? = null,
    ktp_str: reprfunc? = null,
    ktp_getattro: getattrofunc? = null,
    ktp_setattro: setattrofunc? = null,
    ktp_as_buffer: Map<String, CPointer<out CFunction<*>>>? = null,
    ktp_flags: ULong = (Py_TPFLAGS_DEFAULT or Py_TPFLAGS_BASETYPE.toULong()).convert(),
    ktp_doc: String? = null,
    ktp_traverse: traverseproc? = null,
    ktp_clear: inquiry? = null,
    ktp_richcompare: richcmpfunc? = null,
    ktp_weaklistoffset: Py_ssize_t = 0,
    ktp_iter: getiterfunc? = null,
    ktp_iternext: iternextfunc? = null,
    ktp_methods: List<CValue<PyMethodDef>>? = null,
//    ktp_members: List<CValue<PyMemberDef>>? = null,  // TODO: Allow custom members
    ktp_getset: List<CValue<PyGetSetDef>>? = null,
    ktp_base: CPointer<PyTypeObject> = PyBaseObject_Type.ptr,
    ktp_descr_get: descrgetfunc? = null,
    ktp_descr_set: descrsetfunc? = null,
    ktp_init: initproc? = null,
    ktp_alloc: allocfunc? = PyType_GenericAllocKt,
    ktp_new: newfunc? = PyType_GenericNewKt,
    ktp_free: freefunc? = KtType_StableRefFree,
    ktp_is_gc: inquiry? = null,
    ktp_finalize: destructor? = null,
    ktp_vectorcall: vectorcallfunc? = null,
) = makePyType(
    T::class.qualifiedName!!,
    ktp_dealloc,
    ktp_vectorcall_offset,
    ktp_as_async,
    ktp_repr,
    ktp_as_number,
    ktp_as_sequence,
    ktp_as_mapping,
    ktp_hash,
    ktp_call,
    ktp_str,
    ktp_getattro,
    ktp_setattro,
    ktp_as_buffer,
    ktp_flags,
    ktp_doc,
    ktp_traverse,
    ktp_clear,
    ktp_richcompare,
    ktp_weaklistoffset,
    ktp_iter,
    ktp_iternext,
    ktp_methods,
//    ktp_members,
    ktp_getset,
    ktp_base,
    ktp_descr_get,
    ktp_descr_set,
    ktp_init,
    ktp_alloc,
    ktp_new,
    ktp_free,
    ktp_is_gc,
    ktp_finalize,
    ktp_vectorcall
)

fun makePyType(
    name: String,
    ktp_dealloc: destructor? = null,
    ktp_vectorcall_offset: Py_ssize_t = 0,
    ktp_as_async: Map<String, unaryfunc>? = null,
    ktp_repr: reprfunc? = KtType_StableRefRepr,
    ktp_as_number: Map<String, CPointer<out CFunction<*>>>? = null,
    ktp_as_sequence: Map<String, CPointer<out CFunction<*>>>? = null,
    ktp_as_mapping: Map<String, CPointer<out CFunction<*>>>? = null,
    ktp_hash: hashfunc? = null,
    ktp_call: ternaryfunc? = null,
    ktp_str: reprfunc? = null,
    ktp_getattro: getattrofunc? = null,
    ktp_setattro: setattrofunc? = null,
    ktp_as_buffer: Map<String, CPointer<out CFunction<*>>>? = null,
    ktp_flags: ULong = (Py_TPFLAGS_DEFAULT.toULong() or Py_TPFLAGS_BASETYPE.toULong()).convert(),
    ktp_doc: String? = null,
    ktp_traverse: traverseproc? = null,
    ktp_clear: inquiry? = null,
    ktp_richcompare: richcmpfunc? = null,
    ktp_weaklistoffset: Py_ssize_t = 0,
    ktp_iter: getiterfunc? = null,
    ktp_iternext: iternextfunc? = null,
    ktp_methods: List<CValue<PyMethodDef>>? = null,
//    ktp_members: List<CValue<PyMemberDef>>? = null,  // TODO: Allow custom members
    ktp_getset: List<CValue<PyGetSetDef>>? = null,
    ktp_base: CPointer<PyTypeObject> = PyBaseObject_Type.ptr,
    ktp_descr_get: descrgetfunc? = null,
    ktp_descr_set: descrsetfunc? = null,
    ktp_init: initproc? = null,
    ktp_alloc: allocfunc? = PyType_GenericAllocKt,
    ktp_new: newfunc? = PyType_GenericNewKt,
    ktp_free: freefunc? = KtType_StableRefFree,
    ktp_is_gc: inquiry? = null,
    ktp_finalize: destructor? = null,
    ktp_vectorcall: vectorcallfunc? = null,
) = nativeHeap.alloc<PyTypeObject> {
    ob_base.PyVarObject_HEAD_INIT(PyType_Type.ptr, 0)
    tp_name = makeString(name)
    tp_basicsize = sizeOf<KtPyObject>()
    tp_itemsize = 0
    tp_dealloc = ktp_dealloc
    tp_vectorcall_offset = ktp_vectorcall_offset
    tp_getattr = null  // deprecated
    tp_setattr = null  // deprecated
    tp_as_async = ktp_as_async?.let {
        nativeHeap.alloc<PyAsyncMethods> {
            am_await = it["am_await"]
            am_aiter = it["am_aiter"]
            am_anext = it["am_anext"]
        }.ptr
    }
    tp_repr = ktp_repr
    tp_as_number = ktp_as_number?.let {
        nativeHeap.alloc<PyNumberMethods> {
            nb_absolute = it["nb_absolute"]?.reinterpret()
            nb_add = it["nb_add"]!!.reinterpret()
            nb_and = it["nb_and"]?.reinterpret()
            nb_bool = it["nb_bool"]?.reinterpret()
            nb_divmod = it["nb_divmod"]?.reinterpret()
            nb_float = it["nb_float"]?.reinterpret()
            nb_floor_divide = it["nb_floor_divide"]?.reinterpret()
            nb_index = it["nb_index"]?.reinterpret()
            nb_inplace_add = it["nb_inplace_add"]?.reinterpret()
            nb_inplace_and = it["nb_inplace_and"]?.reinterpret()
            nb_inplace_floor_divide = it["nb_inplace_floor_divide"]?.reinterpret()
            nb_inplace_lshift = it["nb_inplace_lshift"]?.reinterpret()
            nb_inplace_matrix_multiply = it["nb_inplace_matrix_multiply"]?.reinterpret()
            nb_inplace_multiply = it["nb_inplace_multiply"]?.reinterpret()
            nb_inplace_or = it["nb_inplace_or"]?.reinterpret()
            nb_inplace_power = it["nb_inplace_power"]?.reinterpret()
            nb_inplace_remainder = it["nb_inplace_remainder"]?.reinterpret()
            nb_inplace_rshift = it["nb_inplace_rshift"]?.reinterpret()
            nb_inplace_subtract = it["nb_inplace_subtract"]?.reinterpret()
            nb_inplace_true_divide = it["nb_inplace_true_divide"]?.reinterpret()
            nb_inplace_xor = it["nb_inplace_xor"]?.reinterpret()
            nb_int = it["nb_int"]?.reinterpret()
            nb_invert = it["nb_invert"]?.reinterpret()
            nb_lshift = it["nb_lshift"]?.reinterpret()
            nb_matrix_multiply = it["nb_matrix_multiply"]?.reinterpret()
            nb_multiply = it["nb_multiply"]?.reinterpret()
            nb_negative = it["nb_negative"]?.reinterpret()
            nb_or = it["nb_or"]?.reinterpret()
            nb_positive = it["nb_positive"]?.reinterpret()
            nb_power = it["nb_power"]?.reinterpret()
            nb_remainder = it["nb_remainder"]?.reinterpret()
            nb_rshift = it["nb_rshift"]?.reinterpret()
            nb_subtract = it["nb_subtract"]?.reinterpret()
            nb_true_divide = it["nb_true_divide"]?.reinterpret()
            nb_xor = it["nb_xor"]?.reinterpret()
        }.ptr
    }
    tp_as_sequence = ktp_as_sequence?.let {
        nativeHeap.alloc<PySequenceMethods> {
            sq_length = it["sq_length"]?.reinterpret()
            sq_concat = it["sq_concat"]?.reinterpret()
            sq_repeat = it["sq_repeat"]?.reinterpret()
            sq_item = it["sq_item"]?.reinterpret()
            sq_ass_item = it["sq_ass_item"]?.reinterpret()
            sq_contains = it["sq_contains"]?.reinterpret()
            sq_inplace_concat = it["sq_inplace_concat"]?.reinterpret()
            sq_inplace_repeat = it["sq_inplace_repeat"]?.reinterpret()
            was_sq_slice = it["was_sq_slice"]?.reinterpret()
            was_sq_ass_slice = it["was_sq_ass_slice"]?.reinterpret()
        }.ptr
    }
    tp_as_mapping = ktp_as_mapping?.let {
        nativeHeap.alloc<PyMappingMethods> {
            mp_length = it["mp_length"]?.reinterpret()
            mp_subscript = it["mp_subscript"]?.reinterpret()
            mp_ass_subscript = it["mp_ass_subscript"]?.reinterpret()
        }.ptr
    }
    tp_hash = ktp_hash
    tp_call = ktp_call
    tp_str = ktp_str
    tp_getattro = ktp_getattro
    tp_setattro = ktp_setattro
    tp_as_buffer = ktp_as_buffer?.let {
        nativeHeap.alloc<PyBufferProcs> {
            bf_getbuffer = it["bf_getbuffer"]?.reinterpret()
            bf_releasebuffer = it["bf_releasebuffer"]?.reinterpret()
        }.ptr
    }
    tp_flags = ktp_flags.convert()
    tp_doc = ktp_doc?.let(::makeString)
    tp_traverse = ktp_traverse
    tp_clear = ktp_clear
    tp_richcompare = ktp_richcompare
    tp_weaklistoffset = ktp_weaklistoffset
    tp_iter = ktp_iter
    tp_iternext = ktp_iternext
    tp_methods = ktp_methods?.let {
        makeArray(*it.toTypedArray())
    }
    tp_members = null
    tp_getset = ktp_getset?.let {
        makeArray(*it.toTypedArray())
    }
    tp_base = ktp_base
    tp_dict = null
    tp_descr_get = ktp_descr_get
    tp_descr_set = ktp_descr_set
    tp_dictoffset = 0
    tp_init = ktp_init
    tp_alloc = ktp_alloc
    tp_new = ktp_new
    tp_free = ktp_free
    tp_is_gc = ktp_is_gc
    tp_finalize = ktp_finalize
    tp_vectorcall = ktp_vectorcall
}
