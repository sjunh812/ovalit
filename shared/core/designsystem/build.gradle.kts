plugins {
    alias(libs.plugins.ovalit.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.ui)
            api(libs.compose.resources)
            api(libs.compose.ui.toolingPreview)
            // 바텀시트와 당겨서 새로고침에 쓴다.
            // MaterialTheme은 Material 컴포넌트가 스스로 고르는 색과 글꼴을 우리 토큰에 맞추려고만 깐다(OvalitTheme.kt).
            implementation(libs.compose.material3)
        }
        androidMain.dependencies {
            implementation(libs.compose.ui.tooling)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.ovalit.core.designsystem.resources"
    generateResClass = always
}
