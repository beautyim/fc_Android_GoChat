plugins {
    alias(libs.plugins.android.library)
}
android {
    namespace = "com.example.demoproject.platform.callkit"
    compileSdk = 36
    defaultConfig { minSdk = 26 }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
dependencies {
    implementation(project(":platform:common"))
    api(project(":platform:rtc-api"))
    implementation(libs.kotlinx.coroutines.android)
}
