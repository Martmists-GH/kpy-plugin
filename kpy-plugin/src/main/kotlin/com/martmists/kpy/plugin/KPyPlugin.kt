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
                val extension = project.the<KPyExtension>()
                val targetName = extension.target ?: targets.filterIsInstance<KotlinNativeTarget>().first().targetName

                sourceSets.getByName("${targetName}Main") {
                    kotlin.srcDir(buildDir.absolutePath + "/generated/ksp/${targetName}/${targetName}Main/kotlin")

                    dependencies {
                        implementation("com.martmists.kpy:kpy-library:${extension.kpyVersion}+${extension.pyVersion.value}")
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
                val targetName = extension.target ?: kotlinExtension.targets.filterIsInstance<KotlinNativeTarget>().first().targetName
                add("ksp${targetName.capitalize()}", "com.martmists.kpy:kpy-processor:${extension.kpyVersion}")
            }
        }
    }

    private fun Project.setupTasks() {
        // Provide setup.py metadata
        tasks.register("setupMetadata") {
            afterEvaluate {
                doLast {
                    val ext = project.the<KPyExtension>()
                    val targetName = ext.target ?: kotlinExtension.targets.filterIsInstance<KotlinNativeTarget>().first().targetName
                    println("""
                    |===METADATA START===
                    |project_name = "${project.name}"
                    |project_version = "${project.version}"
                    |build_dir = "${buildDir.absolutePath}"
                    |target = "$targetName"
                    |has_stubs = ${if (ext.generateStubs) "True" else "False"}
                    ${ext.props.map { (key, value) -> "|$key = $value" }.joinToString { "\n" }}
                    |===METADATA END===
                """.trimMargin()
                    )
                }
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
