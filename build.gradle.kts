import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.martmists.commons.isStable

plugins {
    id("com.github.ben-manes.versions")
    id("se.patrikerdes.use-latest-versions")
}

repositories {
    mavenCentral()
}

allprojects {
    group = "com.martmists.kpy"
    version = "1.0.1"

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

    layout.buildDirectory = rootProject.layout.buildDirectory.dir(name)
}
