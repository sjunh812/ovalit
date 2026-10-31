plugins {
    alias(libs.plugins.ovalit.kmp.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.core.model)
            // RiotJson과 DTO가 공개 API라 쓰는 쪽도 같은 런타임을 본다
            api(libs.kotlinx.serialization.json)
            // 엔진(OkHttp, Darwin)은 앱이 붙일 때 고른다
            api(libs.ktor.client.core)
        }
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
        }
    }
}
