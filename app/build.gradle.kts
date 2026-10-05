plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.stbplay"
    compileSdk = 36

    val releaseKeystorePath = providers.environmentVariable("STB_RELEASE_KEYSTORE").orNull
    if (!releaseKeystorePath.isNullOrBlank()) {
        signingConfigs {
            create("stbRelease") {
                storeFile = file(releaseKeystorePath)
                storePassword = providers.environmentVariable("STB_RELEASE_STORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("STB_RELEASE_KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("STB_RELEASE_KEY_PASSWORD").orNull
            }
        }
    }

    defaultConfig {
        applicationId = "com.example.stbplay"
        minSdk = 26
        targetSdk = 35
        versionCode = 71
        versionName = "2.0.19"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (!releaseKeystorePath.isNullOrBlank()) signingConfig = signingConfigs.getByName("stbRelease")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    
    // Android TV
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)
    
    // Media3
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.cast)
    
    // Persistence & Networking
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    
    // Image Loading
    implementation(libs.coil.compose)
    
    // Serialization
    implementation(libs.kotlinx.serialization.json)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.testmanifest)
    testImplementation("junit:junit:4.13.2")
}
