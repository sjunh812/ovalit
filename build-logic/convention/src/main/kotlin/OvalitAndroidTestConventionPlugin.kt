import com.android.build.api.dsl.TestExtension
import com.ovalit.buildlogic.int
import com.ovalit.buildlogic.javaVersion
import com.ovalit.buildlogic.jvmTarget
import com.ovalit.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

// 다른 모듈의 APK를 띄워 재는 테스트 모듈(:benchmarks)이다.
// AGP 9부터 코틀린 지원이 플러그인에 들어와 있어 kotlin-android를 따로 적용하지 않는다.
class OvalitAndroidTestConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.test")

        extensions.configure<TestExtension> {
            compileSdk = libs.int("androidCompileSdk")

            defaultConfig {
                // Baseline Profile은 안드로이드 9부터 만들 수 있다. 루팅하지 않은 기기는 13부터다.
                minSdk = maxOf(libs.int("androidMinSdk"), 28)
                targetSdk = libs.int("androidTargetSdk")
            }

            compileOptions {
                sourceCompatibility = libs.javaVersion
                targetCompatibility = libs.javaVersion
            }
        }

        extensions.configure<KotlinAndroidProjectExtension> {
            compilerOptions {
                jvmTarget.set(libs.jvmTarget)
            }
        }
    }
}
