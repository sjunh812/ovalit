import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import com.ovalit.buildlogic.int
import com.ovalit.buildlogic.jvmTarget
import com.ovalit.buildlogic.libs
import com.ovalit.buildlogic.ovalitNamespace
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

// AGP 9부터 KMP 공유 모듈은 com.android.library와 호환되지 않는다.
// com.android.kotlin.multiplatform.library를 대신 쓴다.
class OvalitKmpLibraryConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.kotlin.multiplatform.library")

        val moduleNamespace = ovalitNamespace
        val compileSdkVersion = libs.int("androidCompileSdk")
        val minSdkVersion = libs.int("androidMinSdk")
        val jvmTargetVersion = libs.jvmTarget

        extensions.configure<KotlinMultiplatformExtension> {
            (this as ExtensionAware).extensions
                .configure(KotlinMultiplatformAndroidLibraryExtension::class.java) {
                    namespace = moduleNamespace
                    compileSdk = compileSdkVersion
                    minSdk = minSdkVersion
                    // 이 플러그인은 단위 테스트가 opt-in이다. 안 켜면 androidHostTest가 안 생긴다.
                    withHostTestBuilder {}.configure {}
                    // 안드로이드 리소스도 opt-in이다. 안 켜면 CMP 문구가 APK에 안 들어가고
                    // 런타임에 MissingResourceException으로 터진다.
                    androidResources {
                        enable = true
                    }
                }

            // iOS는 시뮬레이터에서 도는 데까지가 범위라 타깃을 하나만 둔다.
            iosSimulatorArm64()

            sourceSets.getByName("commonTest").dependencies {
                implementation(kotlin("test"))
                implementation(libs.findLibrary("kotlinx-coroutines-test").get())
                implementation(libs.findLibrary("turbine").get())
            }

            pluginManager.withPlugin("org.jetbrains.compose") {
                sourceSets.getByName("commonTest").dependencies {
                    implementation(libs.findLibrary("compose-ui-test").get())
                }
            }
        }

        // KMP 안드로이드 타깃은 DSL에 compilerOptions가 없어 컴파일 태스크에서 잡는다.
        tasks.withType<KotlinJvmCompile>().configureEach {
            compilerOptions.jvmTarget.set(jvmTargetVersion)
        }
    }
}
