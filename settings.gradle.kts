pluginManagement {
    includeBuild("build-logic")
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

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "memosly"

include(":app")
include(":core:common")
include(":core:model")
include(":core:network")
include(":core:data")
include(":core:ui")
include(":core:markdown")
include(":feature:auth")
include(":feature:home")
include(":feature:memo")
include(":feature:search")
include(":feature:profile")
include(":feature:explore")
include(":feature:notifications")
