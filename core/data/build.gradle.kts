plugins {
    id("memosly.android.library")
    id("memosly.android.hilt")
}

android {
    namespace = "com.whtis.memosly.core.data"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:network"))

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.moshi)
}
