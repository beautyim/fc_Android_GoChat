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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "demoproject"
include(":app")
include(":platform:common")
include(":platform:network")
include(":platform:data")
include(":platform:analytics")
include(":platform:mqtt")
include(":ui:foundation")
include(":ui:designsystem")
include(":product:feature-home")
include(":platform:s3")
include(":platform:callkit")
include(":platform:rtc-api")
include(":platform:rtc-agora")
include(":product:feature-auth")
include(":product:feature-chat")
include(":product:feature-match")
include(":product:feature-call")
include(":product:feature-store")
include(":product:feature-profile")
include(":product:feature-me")
