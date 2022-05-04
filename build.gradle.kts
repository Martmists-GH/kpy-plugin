import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

buildscript {
    extra["kotlin_plugin_id"] = "com.martmists.kpy-plugin"
}

plugins {
    kotlin("multiplatform") version "1.6.21" apply false
    kotlin("jvm") version "1.6.21" apply false

    id("com.google.devtools.ksp") version "1.6.21-1.0.5" apply false
    id("com.github.gmazzo.buildconfig") version "3.0.3" apply false

    id("com.github.ben-manes.versions") version "0.42.0"
    id("se.patrikerdes.use-latest-versions") version "0.2.18"
}

repositories {
    mavenCentral()
}

allprojects {
    group = "com.martmists.kpy"
    version = "0.2.4"

    tasks.withType<DependencyUpdatesTask> {
        fun isNonStable(version: String): Boolean {
            val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
            val regex = "^[0-9,.v-]+(-r)?$".toRegex()
            val isStable = stableKeyword || regex.matches(version)
            return isStable.not()
        }

        rejectVersionIf {
            isNonStable(candidate.version) && !isNonStable(currentVersion)
        }
    }
}

subprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }

    buildDir = File(rootProject.buildDir.absolutePath + "/" + project.name)
}
