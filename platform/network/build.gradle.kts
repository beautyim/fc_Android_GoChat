import BrandConfig.optional
import BrandConfig.required

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.demoproject.platform.network"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        val props = BrandConfig.load(project, "network")
        buildConfigField("String", "BASE_URL", "\"${BrandConfig.escape(props.required("BASE_URL"))}\"")
        buildConfigField("String", "CLIENT_KEY", "\"${BrandConfig.escape(props.optional("CLIENT_KEY"))}\"")
        buildConfigField("String", "SIGN_KEY", "\"${BrandConfig.escape(props.required("SIGN_KEY"))}\"")
        buildConfigField("String", "ENC_KEY", "\"${BrandConfig.escape(props.required("ENC_KEY"))}\"")
        buildConfigField("String", "UA_PREFIX", "\"${BrandConfig.escape(props.required("UA_PREFIX"))}\"")
        buildConfigField("String", "CHANNEL_NAME", "\"${BrandConfig.escape(props.required("CHANNEL_NAME"))}\"")
        buildConfigField("String", "WEB_VERSION", "\"${BrandConfig.escape(props.required("WEB_VERSION"))}\"")
        buildConfigField("boolean", "ENABLE_REQUEST_SIGN", props.required("ENABLE_REQUEST_SIGN"))
        buildConfigField("boolean", "ENABLE_VERBOSE_HTTP_LOG", "true")
    }

    buildTypes {
        release {
            buildConfigField("boolean", "ENABLE_VERBOSE_HTTP_LOG", "false")
        }
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
    api(libs.retrofit)
    api(libs.retrofit.converter.kotlinx.serialization)
    api(libs.okhttp)
    api(libs.okhttp.logging)
    api(libs.kotlinx.serialization.json)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.hilt.android)
}
