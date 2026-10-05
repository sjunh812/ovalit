plugins {
    alias(libs.plugins.ovalit.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

val IOS_APP_NAME = "Ovalit"
val IOS_EXECUTABLE = "Ovalit"

kotlin {
    // iOS는 Xcode 프로젝트 없이 이 실행 파일을 앱 번들로 묶어 시뮬레이터에 깐다(assembleIosSimulatorApp).
    iosSimulatorArm64 {
        binaries.executable {
            baseName = IOS_EXECUTABLE
            entryPoint = "com.ovalit.app.main"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared.core.model)
            implementation(projects.shared.core.data)
            implementation(projects.shared.core.designsystem)
            implementation(projects.shared.core.ui)
            implementation(projects.shared.feature.onboarding)
            implementation(projects.shared.feature.report)
            implementation(projects.shared.feature.match)
            implementation(projects.shared.feature.friend)
            implementation(projects.shared.feature.profile)
            implementation(projects.shared.feature.settings)

            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.resources)
            implementation(libs.androidx.navigation3.runtime)
            implementation(libs.jb.navigation3.ui)
            implementation(libs.jb.lifecycle.runtime.compose)
            implementation(libs.jb.lifecycle.viewmodel.compose)
            implementation(libs.kotlinx.serialization.core)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
        }
        androidMain.dependencies {
            implementation(libs.androidx.navigation3.ui)
        }
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "com.ovalit.app.resources"
    generateResClass = always
}

// 시뮬레이터에 까는 앱 번들을 만든다. 실행 파일 옆에 Compose 리소스와 Info.plist를 두고 임시 서명만 한다. 기기용 서명과 배포는
// 범위 밖이다(CLAUDE.md 아키텍처).
val iosAppBundle = layout.buildDirectory.dir("ios/$IOS_APP_NAME.app")
val syncIosSimulatorApp = tasks.register<Sync>("syncIosSimulatorApp") {
    val executable = IOS_EXECUTABLE
    dependsOn("linkDebugExecutableIosSimulatorArm64", "iosSimulatorArm64AggregateResources")
    from(layout.buildDirectory.dir("bin/iosSimulatorArm64/debugExecutable")) {
        include("$executable.kexe")
        rename { executable }
    }
    // 모든 모듈의 Compose 리소스(문구, 글꼴)를 모은 것이다. 앱은 번들의 compose-resources에서 읽는다.
    from(layout.buildDirectory.dir("kotlin-multiplatform-resources/aggregated-resources/iosSimulatorArm64")) {
        into("compose-resources")
    }
    from("src/iosMain/Info.plist")
    into(iosAppBundle)
}
tasks.register<Exec>("assembleIosSimulatorApp") {
    group = "build"
    description = "시뮬레이터에 까는 $IOS_APP_NAME.app을 만든다"
    dependsOn(syncIosSimulatorApp)
    commandLine("codesign", "--force", "--sign", "-", iosAppBundle.get().asFile.absolutePath)
}
