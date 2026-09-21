import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.omarea.gesture"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.omarea.gesture.modern"
        minSdk = 26
        targetSdk = 36
        versionCode = 200
        versionName = "2.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keyPropsFile = rootProject.file("key.properties")
            val keyProps = Properties()
            if (keyPropsFile.exists()) {
                keyProps.load(FileInputStream(keyPropsFile))
            }

            val storeFilePath = System.getenv("KEYSTORE_PATH")
                ?: keyProps.getProperty("storeFile")
            val storePass = System.getenv("KEYSTORE_PASSWORD")
                ?: keyProps.getProperty("storePassword")
            val keyAl = System.getenv("KEY_ALIAS")
                ?: keyProps.getProperty("keyAlias")
            val keyPass = System.getenv("KEY_PASSWORD")
                ?: keyProps.getProperty("keyPassword")

            if (!storeFilePath.isNullOrBlank() && file(storeFilePath).exists()) {
                storeFile = file(storeFilePath)
                storePassword = storePass
                keyAlias = keyAl
                keyPassword = keyPass
            } else {
                val debugConfig = signingConfigs.getByName("debug")
                storeFile = debugConfig.storeFile
                storePassword = debugConfig.storePassword
                keyAlias = debugConfig.keyAlias
                keyPassword = debugConfig.keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // Shizuku
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.tooling.preview)
    debugImplementation(libs.compose.tooling)

    testImplementation(libs.junit)
}
