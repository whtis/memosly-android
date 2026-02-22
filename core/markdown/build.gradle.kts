plugins {
    id("memosly.android.library")
    id("memosly.android.compose")
}

android {
    namespace = "com.whtis.memosly.core.markdown"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:ui"))

    implementation(libs.commonmark)
    implementation(libs.commonmark.ext.gfm.tables)
    implementation(libs.commonmark.ext.gfm.strikethrough)
    implementation(libs.commonmark.ext.task.list)
    implementation(libs.commonmark.ext.autolink)

    implementation(libs.coil.compose)
    implementation(libs.compose.material.icons.extended)
}
