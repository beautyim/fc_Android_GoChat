import BrandConfig.escape
import BrandConfig.optional
import BrandConfig.required

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.demoproject"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val props = BrandConfig.load(project, "app")
        applicationId = props.required("APPLICATION_ID")
        buildConfigField("String", "TERMS_URL", "\"${escape(props.optional("TERMS_URL"))}\"")
        buildConfigField("String", "PRIVACY_URL", "\"${escape(props.optional("PRIVACY_URL"))}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                "../config/common/release-log-stripping.pro",
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":platform:common"))
    implementation(project(":platform:network"))
    implementation(project(":platform:data"))
    implementation(project(":platform:analytics"))
    implementation(project(":platform:mqtt"))
    implementation(project(":platform:callkit"))
    implementation(project(":platform:rtc-api"))
    implementation(project(":platform:rtc-agora"))
    implementation(project(":ui:foundation"))
    implementation(project(":ui:designsystem"))
    implementation(project(":product:feature-home"))
    implementation(project(":product:feature-auth"))
    implementation(project(":product:feature-chat"))
    implementation(project(":product:feature-match"))
    implementation(project(":product:feature-call"))
    implementation(project(":product:feature-store"))
    implementation(project(":product:feature-profile"))
    implementation(project(":product:feature-me"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.windowSizeClass)
    implementation(libs.play.billing.ktx)
    implementation(libs.agora.rtc.lite)
    implementation(libs.work.runtime.ktx)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
