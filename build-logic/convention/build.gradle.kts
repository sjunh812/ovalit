import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.ovalit.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// 플러그인은 각 모듈이 적용하고 여기서는 설정 타입만 쓴다. 런타임까지 끌고 가면 플러그인 버전이 두 벌로 갈린다.
dependencies {
    compileOnly(libs.agp.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.composeCompiler.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "ovalit.android.application"
            implementationClass = "OvalitAndroidApplicationConventionPlugin"
        }
        register("kmpLibrary") {
            id = "ovalit.kmp.library"
            implementationClass = "OvalitKmpLibraryConventionPlugin"
        }
    }
}
