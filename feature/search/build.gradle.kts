plugins {
    id("memosly.android.feature")
}

android {
    namespace = "com.whtis.memosly.feature.search"
}

dependencies {
    implementation(project(":core:markdown"))
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    implementation(libs.compose.material.icons.extended)
}
