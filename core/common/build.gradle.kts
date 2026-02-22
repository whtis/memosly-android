plugins {
    id("memosly.android.library")
    id("memosly.android.hilt")
}

android {
    namespace = "com.whtis.memosly.core.common"
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
}
