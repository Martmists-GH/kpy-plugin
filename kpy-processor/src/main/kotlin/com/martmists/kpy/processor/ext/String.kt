package com.martmists.kpy.processor.ext

fun String.toSnakeCase() : String {
    return this.replace('-', '_').lowercase()
}
