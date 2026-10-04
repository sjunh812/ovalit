import java.util.Properties

plugins {
    alias(libs.plugins.ovalit.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

// Google이 공개한 테스트용 ID다. 실제 광고가 아니라 노출과 클릭이 계정에 잡히지 않는다.
val ADMOB_TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
val ADMOB_TEST_NATIVE_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"

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
        // FCM은 google-services.json 대신 이 네 값으로 띄운다. 비밀값은 아니지만 프로젝트마다 달라서 local.properties에서
        // 받는다. 없으면 빈 값이고 앱은 푸시 없이 돈다.
        listOf(
            "FIREBASE_APP_ID" to "firebase.appId",
            "FIREBASE_API_KEY" to "firebase.apiKey",
            "FIREBASE_PROJECT_ID" to "firebase.projectId",
            "FIREBASE_SENDER_ID" to "firebase.senderId",
        ).forEach { (field, key) ->
            buildConfigField("String", field, "\"${localProperties.getProperty(key).orEmpty()}\"")
        }
        // 광고는 AdMob 네이티브 광고 하나다. 광고 단위 ID가 없으면 광고 자리는 비고 광고를 요청하지 않는다. Riot 정책상 제품이
        // 승인되기 전에는 수익을 낼 수 없어서 그때까지 실제 ID를 넣지 않는다(CLAUDE.md 비용). SDK가 앱 ID 없이는 시작하지 않아
        // 앱 ID가 없으면 Google 테스트 앱 ID를 넣어 둔다.
        manifestPlaceholders["admobAppId"] = localProperties.getProperty("admob.appId") ?: ADMOB_TEST_APP_ID
        buildConfigField("String", "ADMOB_NATIVE_UNIT_ID", "\"${localProperties.getProperty("admob.nativeUnitId").orEmpty()}\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        // 디버그 빌드에서 광고 자리를 보려면 local.properties에 admob.test=true를 두거나 -Povalit.ads.test=true로 빌드한다.
        // Google 테스트 광고만 받는다. 릴리스는 이 값을 보지 않는다.
        debug {
            val localProperties = Properties().apply {
                rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
            }
            val testAds = (findProperty("ovalit.ads.test") ?: localProperties.getProperty("admob.test"))?.toString() == "true"
            if (testAds) {
                manifestPlaceholders["admobAppId"] = ADMOB_TEST_APP_ID
                buildConfigField("String", "ADMOB_NATIVE_UNIT_ID", "\"$ADMOB_TEST_NATIVE_UNIT_ID\"")
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        // 릴리스처럼 줄이고 최적화하되 디버그 키로 서명한다. 개발 기기에서 사용자가 느낄 속도를 볼 때 Build Variants에서
        // 고른다. 디버그 빌드는 Compose가 몇 배 느려서 릴리스에 없는 끊김이 보인다.
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }
}

dependencies {
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

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.work.runtime)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.play.services.ads)
    implementation(libs.jb.lifecycle.runtime.compose)
    implementation(libs.kotlinx.serialization.core)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    debugImplementation(libs.compose.ui.tooling)
}
