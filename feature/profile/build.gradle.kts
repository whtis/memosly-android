plugins {
    id("memosly.android.feature")
}

android {
    namespace = "com.whtis.memosly.feature.profile"
}

dependencies {
    implementation(project(":core:network"))
    implementation(libs.compose.material.icons.extended)
    implementation(libs.androidx.appcompat)
}
