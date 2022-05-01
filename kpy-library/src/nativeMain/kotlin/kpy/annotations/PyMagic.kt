package kpy.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class PyMagic(val key: PyMagicMethod)
