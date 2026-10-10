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
        }
    }
}
