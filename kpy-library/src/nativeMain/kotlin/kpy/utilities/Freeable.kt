package kpy.utilities

/**
 * Calls free() when deleted from CPython.
 */
interface Freeable {
    fun free()
}