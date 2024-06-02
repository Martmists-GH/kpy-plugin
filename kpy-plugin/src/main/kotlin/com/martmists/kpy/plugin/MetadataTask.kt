package com.martmists.kpy.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

open class MetadataTask : DefaultTask() {
    @TaskAction
    fun print() {
        val ext = project.the<KPyExtension>()
        val targetName = ext.target.get() ?: (project.kotlinExtension as KotlinMultiplatformExtension).targets.filterIsInstance<KotlinNativeTarget>().first().targetName
        println("""
        |===METADATA START===
        |project_name = "${project.name}"
        |module_name = "${ext.moduleName.get() ?: project.name}"
        |project_version = "${project.version}"
        |build_dir = "${project.layout.buildDirectory.get().asFile.absolutePath}"
        |target = "$targetName"
        |has_stubs = ${if (ext.generateStubs.get()) "True" else "False"}
        ${ext.props.get().map { (key, value) -> "|$key = $value" }.joinToString { "\n" }}
        |===METADATA END===
        """.trimMargin())
    }
}
