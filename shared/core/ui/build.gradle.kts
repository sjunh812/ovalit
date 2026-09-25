plugins {
    alias(libs.plugins.ovalit.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.core.model)
            api(projects.shared.core.designsystem)
        }
    }
}

// 기능 모듈이 기간 이름 같은 공통 문구를 그대로 가져다 쓴다
compose.resources {
    publicResClass = true
    packageOfResClass = "com.ovalit.core.ui.resources"
    generateResClass = always
}
