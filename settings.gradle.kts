enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
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

rootProject.name = "spare"

include(
    ":app",
    ":core:model",
    ":core:data",
    ":core:ui",
    ":scan:api",
    ":scan:mlkit",
    ":capture",
    ":feature:home",
    ":feature:activity",
    ":feature:subscriptions",
    ":feature:insights",
    ":feature:settings",
    ":feature:aibridge",
    ":widget",
)
