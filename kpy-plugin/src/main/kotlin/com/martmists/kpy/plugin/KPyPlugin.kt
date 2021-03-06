package com.martmists.kpy.plugin

import com.google.devtools.ksp.gradle.KspExtension
import com.google.devtools.ksp.gradle.KspGradleSubplugin
import com.martmists.kpy.cfg.BuildConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.targets

open class KPyPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            setupPlugins()
            setupExtensions()
            setupSourceSets()
            setupDependencies()
            setupTasks()
        }
    }

    private fun Project.setupSourceSets() {
        afterEvaluate {
            kotlinExtension.apply {
                targets.filterIsInstance<KotlinNativeTarget>().forEach {
                    sourceSets.getByName("${it.targetName}Main") {
                        kotlin.srcDir(buildDir.absolutePath + "/generated/ksp/${it.targetName}/${it.targetName}Main/kotlin")

                        val extension = the<KPyExtension>()
                        dependencies {
                            implementation("com.martmists.kpy:kpy-library:${extension.kpyVersion}+${extension.pyVersion.value}")
                        }
                    }
                }
            }
        }
    }

    private fun Project.setupPlugins() {
        // Apply KSP plugin
        plugins.apply {
            apply(KspGradleSubplugin::class.java)
        }
    }

    private fun Project.setupDependencies() {
        repositories {
            mavenCentral()
            maven {
                name = "KPy Repository"
                setUrl("https://maven.martmists.com/releases/")
            }
            maven {
                name = "KPy Repository"
                setUrl("https://maven.martmists.com/snapshots/")
            }
        }

        afterEvaluate {
            dependencies.apply {
                val extension = project.the<KPyExtension>()
                kotlinExtension.targets.filterIsInstance<KotlinNativeTarget>().forEach {
                    add("ksp${it.targetName.capitalize()}", "com.martmists.kpy:kpy-processor:${extension.kpyVersion}")
                }
            }
        }
    }

    private fun Project.setupTasks() {
        // Provide setup.py metadata
        tasks.register("setupMetadata") {
            actions.add {
                val target = kotlinExtension.targets.filterIsInstance<KotlinNativeTarget>().first()
                val ext = this@setupTasks.the<KPyExtension>()
                println("""
                |===METADATA START===
                |project_name = "${project.name}"
                |project_version = "${project.version}"
                |root_dir = "${rootDir.absolutePath}"
                |build_dir = "${buildDir.absolutePath}"
                |target = "${target.targetName}"
                |has_stubs = ${if (ext.generateStubs) "True" else "False"}
                ${ext.props.map { (key, value) -> "|$key = $value" }.joinToString { "\n" }}
                |===METADATA END===
                """.trimMargin()
                )
            }
        }
    }

    private fun Project.setupExtensions() {
        val ext = extensions.create<KPyExtension>("kpy")

        afterEvaluate {
            extensions.getByType(KspExtension::class.java).apply {
                arg("projectName", name)
                arg("generateStubs", "${ext.generateStubs}")
            }
        }
    }
}
