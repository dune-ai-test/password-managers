// Aegis — offline, iOS-style password manager.
//
// Repositories are declared explicitly: `com.android.application` resolves ONLY from
// Google Maven; the Kotlin and Compose-compiler plugins resolve from Gradle Plugin
// Portal / Maven Central. google() must come first so AGP never falls back.
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Aegis"
include(":app")
