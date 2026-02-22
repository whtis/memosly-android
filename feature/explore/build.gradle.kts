plugins {
    id("memosly.android.feature")
}

android {
    namespace = "com.whtis.memosly.feature.explore"
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:markdown"))
    implementation(project(":feature:home"))
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    implementation(libs.compose.material.icons.extended)
}
