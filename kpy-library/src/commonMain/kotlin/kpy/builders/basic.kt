package kpy.builders

import kotlinx.cinterop.*
import platform.posix.memcpy

inline fun makeString(content: String): CPointer<ByteVar> {
    val src = content.cstr
    val ptr = nativeHeap.allocArray<ByteVar>(src.size)
    memcpy(ptr, src, src.size.convert())
    return ptr
}

inline fun <reified T : CVariable> makeArray(vararg args: CValue<T>): CPointer<T> {
    val ptr = nativeHeap.allocArray<T>(args.size + 1)
    memScoped {
        args.forEachIndexed { index, cValue ->
            cValue.place(ptr[index].ptr)
        }
        zeroValue<T>().place(ptr[args.size].ptr)
    }
    return ptr
}
