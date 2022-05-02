package kpy.annotations

/**
 * Adds tp_dictoffset to the struct and allocates another void* for the dictptr.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class PyDictClass
