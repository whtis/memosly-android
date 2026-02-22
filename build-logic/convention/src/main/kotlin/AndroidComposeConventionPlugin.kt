import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            val extension = extensions.findByType(com.android.build.api.dsl.ApplicationExtension::class.java)
                ?: extensions.findByType(com.android.build.gradle.LibraryExtension::class.java)
                ?: error("Android plugin not applied")

            (extension as CommonExtension<*, *, *, *, *, *>).apply {
                buildFeatures {
                    compose = true
                }
            }

            dependencies {
                val bom = versionCatalog.findLibrary("compose-bom").get()
                add("implementation", platform(bom))
                add("implementation", versionCatalog.findLibrary("compose-ui").get())
                add("implementation", versionCatalog.findLibrary("compose-ui-graphics").get())
                add("implementation", versionCatalog.findLibrary("compose-ui-tooling-preview").get())
                add("implementation", versionCatalog.findLibrary("compose-material3").get())
                add("implementation", versionCatalog.findLibrary("compose-foundation").get())
                add("debugImplementation", versionCatalog.findLibrary("compose-ui-tooling").get())
            }
        }
    }
}
