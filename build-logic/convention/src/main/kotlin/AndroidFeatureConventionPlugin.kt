import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("memosly.android.library")
                apply("memosly.android.compose")
                apply("memosly.android.hilt")
            }

            dependencies {
                add("implementation", project(":core:common"))
                add("implementation", project(":core:model"))
                add("implementation", project(":core:data"))
                add("implementation", project(":core:ui"))

                add("implementation", versionCatalog.findLibrary("androidx-lifecycle-runtime-compose").get())
                add("implementation", versionCatalog.findLibrary("androidx-lifecycle-viewmodel-compose").get())
                add("implementation", versionCatalog.findLibrary("androidx-navigation-compose").get())
                add("implementation", versionCatalog.findLibrary("hilt-navigation-compose").get())
                add("implementation", versionCatalog.findLibrary("kotlinx-coroutines-android").get())
            }
        }
    }
}
