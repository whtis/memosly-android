plugins {
    id("memosly.android.library")
    id("memosly.android.hilt")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.whtis.memosly.core.network"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.codegen)

    implementation(libs.kotlinx.coroutines.android)
}
