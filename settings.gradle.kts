pluginManagement {
    repositories {
        // Avoid probing Google Maven for every Kotlin/JVM dependency.
        // https://docs.gradle.org/current/userguide/filtering_repository_content.html
        google {
            content {
                includeGroupByRegex("androidx\\..*")
                includeGroupByRegex("com\\.android(\\..*)?")
                includeGroupByRegex("com\\.google(\\..*)?")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("androidx\\..*")
                includeGroupByRegex("com\\.android(\\..*)?")
                includeGroupByRegex("com\\.google(\\..*)?")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "duolingo-kmp-learning-app"
include(":composeApp")
