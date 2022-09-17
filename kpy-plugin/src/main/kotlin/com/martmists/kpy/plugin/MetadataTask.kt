package com.martmists.kpy.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.targets

open class MetadataTask : DefaultTask() {
    @TaskAction
    fun print() {
        val ext = project.the<KPyExtension>()
        val targetName = ext.target.get() ?: project.kotlinExtension.targets.filterIsInstance<KotlinNativeTarget>().first().targetName
        println("""
        |===METADATA START===
        |project_name = "${ext.moduleName.get() ?: project.name}"
        |project_version = "${project.version}"
        |build_dir = "${project.buildDir.absolutePath}"
        |target = "$targetName"
        |has_stubs = ${if (ext.generateStubs.get()) "True" else "False"}
        ${ext.props.get().map { (key, value) -> "|$key = $value" }.joinToString { "\n" }}
        |===METADATA END===
        """.trimMargin())
    }
}
