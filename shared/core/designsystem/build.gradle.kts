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
            // MaterialTheme은 쓰지 않는다. ripple 하나 때문에 들인다.
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
