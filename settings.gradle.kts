rootProject.name = "ovalit"

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
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
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":composeApp")

include(":shared:core:model")
include(":shared:core:network")
include(":shared:core:data")
include(":shared:core:designsystem")

include(":shared:feature:onboarding")
include(":shared:feature:report")
include(":shared:feature:match")
include(":shared:feature:friend")
include(":shared:feature:settings")
include(":shared:feature:profile")
