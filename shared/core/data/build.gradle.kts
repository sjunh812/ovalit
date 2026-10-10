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
        // core/testing이 이 모듈의 인터페이스를 구현하니 테스트에서만 건다. main에 걸면 서로를 물고 돈다.
        commonTest.dependencies {
            implementation(projects.shared.core.testing)
        }
    }
}
