import com.android.build.api.dsl.ApplicationExtension
import com.ovalit.buildlogic.int
import com.ovalit.buildlogic.javaVersion
import com.ovalit.buildlogic.jvmTarget
import com.ovalit.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

// AGP 9부터 코틀린 지원이 플러그인에 들어와 있어 kotlin-android를 따로 적용하지 않는다.
class OvalitAndroidApplicationConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")

        extensions.configure<ApplicationExtension> {
            compileSdk = libs.int("androidCompileSdk")

            defaultConfig {
                minSdk = libs.int("androidMinSdk")
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
