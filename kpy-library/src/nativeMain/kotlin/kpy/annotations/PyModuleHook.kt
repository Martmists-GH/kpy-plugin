package kpy.annotations

/**
 * Hook into initialization of the generated module
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class PyModuleHook
