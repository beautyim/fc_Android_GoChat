import BrandConfig.required

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.demoproject.platform.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        val props = BrandConfig.load(project, "network")
        buildConfigField("String", "PIC_CDN_BASE_URL", "\"${BrandConfig.escape(props.required("PIC_CDN_BASE_URL"))}\"")
        buildConfigField("String", "ASSET_CDN_BASE_URL", "\"${BrandConfig.escape(props.required("ASSET_CDN_BASE_URL"))}\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":platform:common"))
    api(project(":platform:network"))
    api(project(":platform:s3"))
    implementation(project(":platform:mqtt"))
    implementation(libs.hilt.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.sqlite)
    ksp(libs.room.compiler)
    implementation(libs.sqlcipher.android)
    implementation(libs.tink.android)

    testImplementation(libs.junit)
}
