package kpy.annotations

/**
 * Export the annotated function/class/method
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
@Suppress("unused")
annotation class PyExport(val name: String = "", val priority: Int = 9999)
