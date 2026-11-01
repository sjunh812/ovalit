plugins {
    alias(libs.plugins.ovalit.android.test)
    alias(libs.plugins.baselineprofile)
}

// 앱을 따로 띄워 시작 시간과 프레임을 재고 Baseline Profile을 만든다(docs/CONVENTIONS.md 빌드).
// 앱의 릴리스 빌드 타입을 따라 이 모듈에도 nonMinifiedRelease와 benchmarkRelease 변형이 생기고 각각 같은 이름의 앱 빌드를 띄운다.
android {
    namespace = "com.ovalit.benchmarks"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":composeApp"
    // 재는 앱을 죽였다 켤 때 테스트 APK가 같이 죽지 않게 따로 띄운다.
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

baselineProfile {
    // 붙어 있는 기기에서 만든다.
    // Gradle 관리 기기는 시스템 이미지를 따로 받아야 해서 쓰지 않는다.
    // 연결된 기기를 모두 쓰니 ANDROID_SERIAL로 에뮬레이터만 고른다.
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.uiautomator)
}
