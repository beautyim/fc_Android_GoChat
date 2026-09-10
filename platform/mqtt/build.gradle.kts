plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.demoproject.platform.mqtt"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        buildConfigField("boolean", "ENABLE_VERBOSE_MQTT_LOG", "true")
    }


    buildTypes {
        debug {
            buildConfigField("boolean", "ENABLE_VERBOSE_MQTT_LOG", "true")
        }
        release {
            buildConfigField("boolean", "ENABLE_VERBOSE_MQTT_LOG", "false")
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
    implementation(project(":platform:callkit"))
    implementation(project(":platform:network"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.datastore.preferences)
    implementation(libs.paho.mqttv3)
}
