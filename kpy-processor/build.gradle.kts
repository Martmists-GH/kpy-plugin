import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("com.github.gmazzo.buildconfig")
    `maven-publish`
}

dependencies {
    kapt("com.google.auto.service:auto-service:1.1.1")
    implementation("com.google.auto.service:auto-service-annotations:1.1.1")

    implementation("com.google.devtools.ksp:symbol-processing-api:2.0.0-1.0.21")
}

buildConfig {
    packageName.set("com.martmists.kpy.cfg")
    buildConfigField("String", "VERSION", "\"${project.version}\"")
}

tasks {
    withType<KotlinCompile> {
        dependsOn("generateBuildConfig")

        compilerOptions {
            freeCompilerArgs = listOf("-Xcontext-receivers")
        }
    }
}

if (findProperty("mavenToken") != null) {
    publishing {
        repositories {
            maven {
                name = "Host"
                url = uri("https://maven.martmists.com/releases")
                credentials {
                    username = "admin"
                    password = project.ext["mavenToken"]!! as String
                }
            }
        }

        publications {
            create<MavenPublication>("jvm") {
                groupId = project.group as String
                artifactId = project.name
                version = project.version as String

                from(components["java"])
            }
        }
    }
} else if (System.getenv("CI") == "true") {
    publishing {
        repositories {
            maven {
                name = "Host"
                url = uri(System.getenv("GITHUB_TARGET_REPO")!!)
                credentials {
                    username = "kpy-actions"
                    password = System.getenv("DEPLOY_KEY")!!
                }
            }
        }

        publications {
            create<MavenPublication>("jvm") {
                groupId = project.group as String
                artifactId = project.name
                version = project.version as String

                from(components["java"])
            }
        }

        publications.withType<MavenPublication> {
            if (System.getenv("DEPLOY_TYPE") == "snapshot") {
                version = System.getenv("GITHUB_SHA")!!
            }
        }
    }
}

