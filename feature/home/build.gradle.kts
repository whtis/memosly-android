plugins {
    id("memosly.android.feature")
}

android {
    namespace = "com.whtis.memosly.feature.home"
}

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:markdown"))
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    implementation(libs.coil.compose)
    implementation(libs.compose.material.icons.extended)
}
