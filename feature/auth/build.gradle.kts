plugins {
    id("memosly.android.feature")
}

android {
    namespace = "com.whtis.memosly.feature.auth"
}

dependencies {
    implementation(project(":core:network"))
    implementation(libs.compose.material.icons.extended)
}
