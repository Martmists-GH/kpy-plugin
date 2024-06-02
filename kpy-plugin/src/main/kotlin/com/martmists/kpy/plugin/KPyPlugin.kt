package com.martmists.kpy.plugin

import com.google.devtools.ksp.gradle.KspExtension
import com.google.devtools.ksp.gradle.KspGradleSubplugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

@Suppress("unused")
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
                val targetName = extension.target.get() ?: (this as KotlinMultiplatformExtension).targets.filterIsInstance<KotlinNativeTarget>().first().targetName

                sourceSets.getByName("${targetName}Main") {
                    kotlin.srcDir(layout.projectDirectory.dir("/generated/ksp/${targetName}/${targetName}Main/kotlin"))

                    dependencies {
                        implementation("com.martmists.kpy:kpy-library:${extension.kpyVersion.get()}+${extension.pyVersion.get().value}")
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
                val targetName = extension.target.get() ?: (kotlinExtension as KotlinMultiplatformExtension).targets.filterIsInstance<KotlinNativeTarget>().first().targetName
                add("ksp${targetName.capitalized()}", "com.martmists.kpy:kpy-processor:${extension.kpyVersion.get()}")
            }
        }
    }

    private fun Project.setupTasks() {
        // Provide setup.py metadata
        tasks {
            val setupMetadata by registering(MetadataTask::class)
        }
    }

    private fun Project.setupExtensions() {
        val ext = extensions.create<KPyExtension>("kpy")

        afterEvaluate {
            extensions.getByType(KspExtension::class.java).apply {
                arg("projectName", name)
                arg("generateStubs", "${ext.generateStubs.get()}")
            }
        }
    }
}
