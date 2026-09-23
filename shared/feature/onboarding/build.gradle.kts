plugins {
    alias(libs.plugins.ovalit.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared.core.designsystem)
        }
        androidMain.dependencies {
            implementation(libs.compose.ui.tooling)
        }
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "com.ovalit.feature.onboarding.resources"
    generateResClass = always
}
