package com.martmists.kpy.processor.ext

fun String.toSnakeCase() : String {
    return this.replace('-', '_').lowercase()
}

fun String.toKPyName() : String {
    // TODO: Add custom handling for each
    return when(this) {
        "__getattr__" -> "ktp_getattro"
        "__setattr__" -> "ktp_setattro"
        "__repr__" -> "ktp_repr"
        "__hash__" -> "ktp_hash"
        "__call__" -> "ktp_call"
        "__str__" -> "ktp_str"
        "__iter__" -> "ktp_iter"
        "__next__" -> "ktp_iternext"
        "__get__" -> "ktp_descr_get"
        "__set__" -> "ktp_descr_set"
        "__new__" -> "ktp_new"
        "__del__" -> "ktp_finalize"
        else -> throw NotImplementedError("Not implemented special method: $this")
    }
}
