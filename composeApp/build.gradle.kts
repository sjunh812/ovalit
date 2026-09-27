import java.util.Properties

plugins {
    alias(libs.plugins.ovalit.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.ovalit"

    defaultConfig {
        applicationId = "com.ovalit"
        versionCode = 1
        versionName = "0.1.0"
        // RSO 로그인을 마치면 서버가 돌려보내는 App Link의 호스트다(server/README.md). 비밀값이 아니어도 배포 주소가
        // 정해지기 전이라 local.properties에서 받는다. 없으면 아무 데도 이어지지 않는 예약 도메인을 쓴다.
        val localProperties = Properties().apply {
            rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
        }
        manifestPlaceholders["ovalitServerHost"] = localProperties.getProperty("ovalit.server.host") ?: "ovalit.invalid"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    implementation(projects.shared.core.data)
    implementation(projects.shared.core.designsystem)
    implementation(projects.shared.feature.onboarding)
    implementation(projects.shared.feature.report)
    implementation(projects.shared.feature.match)
    implementation(projects.shared.feature.friend)
    implementation(projects.shared.feature.profile)
    implementation(projects.shared.feature.settings)

    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.work.runtime)
    implementation(libs.jb.lifecycle.runtime.compose)
    implementation(libs.kotlinx.serialization.core)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    debugImplementation(libs.compose.ui.tooling)
}
