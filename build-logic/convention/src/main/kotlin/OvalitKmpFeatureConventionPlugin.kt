import com.ovalit.buildlogic.libs
import com.ovalit.buildlogic.ovalitNamespace
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

// shared/feature 아래 모듈이 같이 쓴다. 기능 모듈은 core에만 기대고 서로를 모른다. 어디서 어디로 가는지는 shared/app이 정한다.
class OvalitKmpFeatureConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("ovalit.kmp.library")
        pluginManager.apply("org.jetbrains.compose")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.getByName("commonMain").dependencies {
                implementation(project(":shared:core:data"))
                implementation(project(":shared:core:designsystem"))
                implementation(project(":shared:core:ui"))
                implementation(libs.findLibrary("jb-lifecycle-runtime-compose").get())
                implementation(libs.findLibrary("jb-lifecycle-viewmodel").get())
                implementation(project.dependencies.platform(libs.findLibrary("koin-bom").get()))
                implementation(libs.findLibrary("koin-compose-viewmodel").get())
            }
            sourceSets.getByName("androidMain").dependencies {
                implementation(libs.findLibrary("compose-ui-tooling").get())
            }
        }

        // 앱은 모든 모듈의 Compose 리소스를 이 패키지 이름의 폴더로 모은다. 모듈마다 달라야 해서 네임스페이스에서 만든다
        // (:shared:feature:report → com.ovalit.feature.report.resources).
        val resourcePackage = "$ovalitNamespace.resources"
        extensions.configure<ComposeExtension> {
            (this as ExtensionAware).extensions.configure(ResourcesExtension::class.java) {
                publicResClass = false
                packageOfResClass = resourcePackage
                // compose-resources가 designsystem을 거쳐 들어와서 기본값(auto)으로는 Res 클래스가 생기지 않는다.
                generateResClass = ResourcesExtension.ResourceClassGeneration.Always
            }
        }
    }
}
