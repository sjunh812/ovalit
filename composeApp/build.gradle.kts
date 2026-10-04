import java.util.Properties

plugins {
    alias(libs.plugins.ovalit.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

// Firebase 콘솔이 주는 google-services.json이 있을 때만 Firebase를 붙인다. 공개 저장소라 이 파일은 올리지 않아서(.gitignore),
// 없는 체크아웃에서도 빌드되고 앱은 푸시, 사용 통계, 비정상 종료 보고 없이 돈다.
val firebaseConfigured = file("google-services.json").exists()
if (firebaseConfigured) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
    apply(plugin = libs.plugins.firebase.crashlytics.get().pluginId)
}

// Google이 공개한 테스트용 ID다. 실제 광고가 아니라 노출과 클릭이 계정에 잡히지 않는다.
val ADMOB_TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
val ADMOB_TEST_NATIVE_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
val ADMOB_TEST_REWARDED_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

// local.properties의 값이다. 키만 적고 값을 비워 둔 것도 없는 것으로 친다. 빈 admob.appId가 그대로 들어가면 광고 SDK가 앱
// 시작 때 죽는다.
fun localProperty(key: String): String? = localProperties.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() }

android {
    namespace = "com.ovalit"

    defaultConfig {
        applicationId = "com.ovalit"
        versionCode = 1
        versionName = "0.1.0"
        // RSO 로그인을 마치면 서버가 돌려보내는 App Link의 호스트다(server/README.md). 비밀값은 아니지만 배포 주소가
        // 정해지기 전이라 local.properties에서 받는다. 없으면 아무 데도 이어지지 않는 예약 도메인을 쓴다.
        manifestPlaceholders["ovalitServerHost"] = localProperty("ovalit.server.host") ?: "ovalit.invalid"
        buildConfigField("boolean", "FIREBASE_ENABLED", firebaseConfigured.toString())
        // 광고 단위 ID가 없으면 광고를 요청하지 않는다. 제품이 승인되기 전에는 수익을 낼 수 없어 실제 ID를 넣지 않는다
        // (CLAUDE.md 지켜야 할 선). SDK는 앱 ID가 없으면 시작하지 않아서 그때는 Google 테스트 앱 ID를 넣는다.
        manifestPlaceholders["admobAppId"] = localProperty("admob.appId") ?: ADMOB_TEST_APP_ID
        buildConfigField("String", "ADMOB_NATIVE_UNIT_ID", "\"${localProperty("admob.nativeUnitId").orEmpty()}\"")
        // "24시간 광고 없이 보기"에 쓰는 보상형 광고다. 없으면 광고 줄의 ×가 그 광고만 바로 닫고 설정 줄이 없다.
        buildConfigField("String", "ADMOB_REWARDED_UNIT_ID", "\"${localProperty("admob.rewardedUnitId").orEmpty()}\"")
        // 설정의 "피드백 보내기"가 여는 메일의 받는 주소다. 개인 주소를 저장소에 넣지 않으려고 local.properties에서 받고, 없으면
        // 그 줄이 없다.
        buildConfigField("String", "FEEDBACK_EMAIL", "\"${localProperty("ovalit.feedback.email").orEmpty()}\"")
    }

    // 안드로이드 13부터 앱 설정에서 앱만 따로 일본어로 고를 수 있게 지원 언어 목록을 만든다. 기본 values는 한국어다
    // (src/main/res/resources.properties).
    androidResources {
        generateLocaleConfig = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        // 디버그 빌드에서 광고 자리를 보려면 local.properties에 admob.test=true를 두거나 -Povalit.ads.test=true로 빌드한다.
        // Google 테스트 광고만 받는다. 릴리스는 이 값을 보지 않는다.
        debug {
            val testAds = (findProperty("ovalit.ads.test") ?: localProperty("admob.test"))?.toString() == "true"
            if (testAds) {
                manifestPlaceholders["admobAppId"] = ADMOB_TEST_APP_ID
                buildConfigField("String", "ADMOB_NATIVE_UNIT_ID", "\"$ADMOB_TEST_NATIVE_UNIT_ID\"")
                buildConfigField("String", "ADMOB_REWARDED_UNIT_ID", "\"$ADMOB_TEST_REWARDED_UNIT_ID\"")
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
            // 개발 기기에서 속도를 보는 빌드라 Crashlytics에 매핑 파일을 올리지 않는다. 올리는 건 릴리스뿐이다.
            (this as ExtensionAware).extensions.findByName("firebaseCrashlytics")?.withGroovyBuilder {
                setProperty("mappingFileUploadEnabled", false)
            }
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
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.play.services.ads)
    implementation(libs.jb.lifecycle.runtime.compose)
    implementation(libs.kotlinx.serialization.core)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    debugImplementation(libs.compose.ui.tooling)
}
