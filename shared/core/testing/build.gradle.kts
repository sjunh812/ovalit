// 여러 테스트가 같이 쓰는 대역을 둔다. 테스트 의존성으로만 걸고 앱에는 들어가지 않는다.
plugins {
    alias(libs.plugins.ovalit.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.core.data)
        }
    }
}
