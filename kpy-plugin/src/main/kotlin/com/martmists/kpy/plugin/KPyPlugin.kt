package com.martmists.kpy.plugin

import com.google.devtools.ksp.gradle.KspExtension
import com.google.devtools.ksp.gradle.KspGradleSubplugin
import com.martmists.kpy.cfg.BuildConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.task
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension

class KPyPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            setupSourceSets()
            setupPlugins()
            setupDependencies()
            setupTasks()
            setupExtensions()
        }
    }

    context(Project)
    fun setupSourceSets() {
        kotlinExtension.sourceSets.filter {
            it.name == "nativeMain"
        }.forEach {
            // KSP generated source code
            it.kotlin.srcDir(buildDir.absolutePath + "/generated/ksp/native/nativeMain/kotlin")

            // KPy Library
            it.dependencies {
                implementation("com.martmists:kpy-library:${BuildConfig.VERSION}")
            }
        }
    }

    context(Project)
    fun setupPlugins() {
        // Apply KSP plugin
        plugins.apply {
            apply(KspGradleSubplugin::class.java)
        }
    }

    context(Project)
    fun setupDependencies() {
        dependencies.apply {
            // Setup KSP processor
            add("kspNative", "com.martmists:kpy-processor:${BuildConfig.VERSION}")
        }
    }

    context(Project)
    fun setupTasks() {
        // Provide setup.py metadata
        task<Task>("setupMetadata") {
            doLast {
                println("""
                    |===METADATA START===
                    |project_name = "$name"
                    |project_version = "$version"
                    |build_dir = "${buildDir.absolutePath}"
                    ${the<KPyExtension>().props.map { (key, value) -> "|$key = $value" }.joinToString { "\n" }}
                    |===METADATA END===
                """.trimMargin())
            }
        }
    }

    context(Project)
    fun setupExtensions() {
        extensions.create<KPyExtension>("kpy")

        extensions.getByType(KspExtension::class.java).apply {
            arg("projectName", name)
        }
    }
}
