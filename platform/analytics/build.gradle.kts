import BrandConfig.optional

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.demoproject.platform.analytics"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
        val props = BrandConfig.load(project, "adjust")
        fun stringField(name: String, key: String, default: String = "") {
            buildConfigField(
                "String",
                name,
                "\"${BrandConfig.escape(props.optional(key, default))}\"",
            )
        }
        stringField("ADJUST_APP_TOKEN", "ADJUST_APP_TOKEN")
        stringField("ADJUST_EVENT_ACTIVE", "ADJUST_EVENT_ACTIVE")
        stringField("ADJUST_EVENT_FIRST_DIALOG", "ADJUST_EVENT_FIRST_DIALOG")
        stringField("ADJUST_EVENT_ORDER_SUBMIT", "ADJUST_EVENT_ORDER_SUBMIT")
        stringField("ADJUST_EVENT_PAY", "ADJUST_EVENT_PAY")
        stringField("ADJUST_EVENT_REGISTER", "ADJUST_EVENT_REGISTER")
        buildConfigField(
            "boolean",
            "ADJUST_ENABLED",
            props.optional("ADJUST_ENABLED", "true"),
        )
        stringField("ADJUST_ENVIRONMENT", "ADJUST_ENVIRONMENT", "production")
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.adjust.android)
    implementation(libs.android.installreferrer)
    implementation(libs.play.services.ads.identifier)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
}
