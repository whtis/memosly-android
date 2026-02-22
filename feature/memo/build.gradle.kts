plugins {
    id("memosly.android.feature")
}

android {
    namespace = "com.whtis.memosly.feature.memo"
}

dependencies {
    implementation(project(":core:markdown"))
    implementation(project(":core:network"))
    implementation(libs.coil.compose)
    implementation(libs.compose.material.icons.extended)
}
