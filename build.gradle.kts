import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.martmists.commons.isStable

buildscript {
    extra["kotlin_plugin_id"] = "com.martmists.kpy-plugin"
}

plugins {
    kotlin("multiplatform") version "1.7.0" apply false
    kotlin("jvm") version "1.7.0" apply false

    id("com.google.devtools.ksp") version "1.7.0-1.0.6" apply false
    id("com.github.gmazzo.buildconfig") version "3.0.3" apply false

    id("com.github.ben-manes.versions") version "0.42.0"
    id("se.patrikerdes.use-latest-versions") version "0.2.18"
}

repositories {
    mavenCentral()
}

allprojects {
    group = "com.martmists.kpy"
    version = "0.3.7-1.7.0"

    tasks.withType<DependencyUpdatesTask> {
        rejectVersionIf {
            isStable(currentVersion) && !isStable(candidate.version)
        }
    }
}

subprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }

    buildDir = file(rootProject.buildDir.absolutePath + "/" + project.name)
}
