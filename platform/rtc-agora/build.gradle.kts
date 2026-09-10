plugins {
    alias(libs.plugins.android.library)
}
android {
    namespace = "com.example.demoproject.platform.rtc.agora"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
dependencies {
    implementation(project(":platform:common"))
    api(project(":platform:rtc-api"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    compileOnly(libs.agora.rtc.lite)
}
