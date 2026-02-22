plugins {
    id("memosly.android.library")
}

android {
    namespace = "com.whtis.memosly.core.model"
}

dependencies {
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
}
