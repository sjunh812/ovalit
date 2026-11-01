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

// 앱 시작과 홈 프레임을 재고 Baseline Profile을 만든다. 앱에 들어가지 않는다.
include(":benchmarks")

// 화면 전환 뼈대. 안드로이드는 composeApp이 감싸고, iOS는 이 모듈이 바로 시뮬레이터용 앱이 된다.
include(":shared:app")

include(":shared:core:model")
include(":shared:core:network")
include(":shared:core:data")
include(":shared:core:designsystem")
include(":shared:core:ui")
include(":shared:core:testing")

include(":shared:feature:onboarding")
include(":shared:feature:report")
include(":shared:feature:match")
include(":shared:feature:friend")
include(":shared:feature:settings")
include(":shared:feature:profile")
