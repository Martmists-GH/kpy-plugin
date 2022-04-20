package com.martmists.kpy.plugin

import com.martmists.kpy.cfg.BuildConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension

class KPyPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.kotlinExtension.sourceSets.filter {
            true
        }.forEach {
            it.dependencies {
                implementation("com.martmists:kpy-library:${BuildConfig.VERSION}")            }
        }

        target.dependencies.apply {
            add("kspNative", "com.martmists:kpy-processor:${BuildConfig.VERSION}")
        }
    }
}
