plugins {
    alias(libs.plugins.ovalit.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.core.model)
            api(libs.kotlinx.coroutines.core)
            implementation(libs.androidx.datastore.preferences.core)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
        }
    }
}
