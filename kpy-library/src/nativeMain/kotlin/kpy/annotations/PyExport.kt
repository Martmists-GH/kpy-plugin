package kpy.annotations

/**
 * Export the annotated function/class/method
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class PyExport(val name: String = "")
