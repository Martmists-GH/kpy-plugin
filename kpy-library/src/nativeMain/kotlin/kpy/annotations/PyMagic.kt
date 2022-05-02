package kpy.annotations

/**
 * Export this method as the specified magic method
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class PyMagic(val key: PyMagicMethod)
