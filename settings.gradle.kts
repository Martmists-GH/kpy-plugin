pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

buildscript {
    repositories {
        mavenCentral()
        maven("https://maven.martmists.com/releases")
    }
    dependencies {
        classpath("com.martmists.commons:commons-gradle:1.0.1")
    }
}

include(":kpy-library")
include(":kpy-plugin")
include(":kpy-processor")
include(":kpy-sample")
