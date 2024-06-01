package kpy.annotations

/**
 * Add this property to the generated stubs
 * Note: This does not generate the code necessary to access it,
 *  you should implement a custom TP_GETATTRO for that.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class PyHint
