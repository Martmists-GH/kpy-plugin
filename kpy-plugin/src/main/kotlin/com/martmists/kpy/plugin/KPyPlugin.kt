package com.martmists.kpy.plugin

import com.google.devtools.ksp.gradle.KspExtension
import com.google.devtools.ksp.gradle.KspGradleSubplugin
import com.martmists.kpy.cfg.BuildConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.targets

open class KPyPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            setupExtensions()
            setupSourceSets()
            setupPlugins()
            setupDependencies()
            setupTasks()
        }
    }

    private fun Project.setupSourceSets() {
        kotlinExtension.apply {
            targets.filterIsInstance<KotlinNativeTarget>().forEach {
                afterEvaluate {
                    sourceSets.getByName("${it.targetName}Main") {
                        kotlin.srcDir(buildDir.absolutePath + "/generated/ksp/${it.targetName}/${it.targetName}Main/kotlin")

                        val extension = the<KPyExtension>()
                        dependencies {
                            implementation("com.martmists.kpy:kpy-library:${BuildConfig.VERSION}+${extension.pyVersion}")
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
        }

        dependencies.apply {
            kotlinExtension.targets.forEach {
                add("ksp${it.targetName.capitalize()}", "com.martmists.kpy:kpy-processor:${BuildConfig.VERSION}")
            }
        }
    }

    private fun Project.setupTasks() {
        // Provide setup.py metadata
        task<Task>("setupMetadata") {
            actions.add {
                val target = kotlinExtension.targets.first { it is KotlinNativeTarget } as KotlinNativeTarget
                println("""
                    |===METADATA START===
                    |project_name = "$name"
                    |project_version = "$version"
                    |build_dir = "${buildDir.absolutePath}"
                    |bin_dir = "${buildDir.absolutePath}/bin/${target.targetName}"
                    ${the<KPyExtension>().props.map { (key, value) -> "|$key = $value" }.joinToString { "\n" }}
                    |===METADATA END===
                """.trimMargin())
            }
        }
    }

    private fun Project.setupExtensions() {
        extensions.create<KPyExtension>("kpy")

        extensions.getByType(KspExtension::class.java).apply {
            arg("projectName", name)
        }
    }
}
