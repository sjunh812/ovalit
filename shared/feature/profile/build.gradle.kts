plugins {
    alias(libs.plugins.ovalit.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared.core.data)
            implementation(projects.shared.core.designsystem)
            implementation(projects.shared.core.ui)
            implementation(libs.jb.lifecycle.runtime.compose)
            implementation(libs.jb.lifecycle.viewmodel)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.compose.viewmodel)
        }
        androidMain.dependencies {
            implementation(libs.compose.ui.tooling)
        }
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "com.ovalit.feature.profile.resources"
    generateResClass = always
}
