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

// compileOnly인 이유. 여기서는 다른 플러그인의 설정 타입 이름만 가져다 쓰고, 플러그인을
// 실제로 적용하는 건 각 모듈이다. runtime에 끌고 가면 버전이 두 벌로 갈린다.
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
