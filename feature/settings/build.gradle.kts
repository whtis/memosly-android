plugins {
    id("memosly.android.feature")
}

android {
    namespace = "com.whtis.memosly.feature.settings"
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.compose.material.icons.extended)
}
