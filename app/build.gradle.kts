plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.ksp) // Replaced kapt with ksp
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.jetbrains.kotlin.compose)
    id("kotlin-parcelize")
}

android {
    namespace = "com.example.xdlocker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.xdlocker"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11 // Or VERSION_17
        targetCompatibility = JavaVersion.VERSION_11 // Or VERSION_17
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }

    // Remove the explicit composeOptions block if you are using the Compose Compiler plugin
    // The plugin handles the compiler extension version automatically based on its own version.
    // composeOptions {
    //     kotlinCompilerExtensionVersion = "1.5.8" 
    // }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)

    // Material Components for XML themes (THIS IS THE FIX)
    implementation(libs.material)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.material3)
    // Compose Material Icons (For icon issues)
    implementation(libs.androidx.compose.material.icons.core)     // <<< For Icons
    implementation(libs.androidx.compose.material.icons.extended) // <<< For Icons

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler) // Changed from kapt to ksp

    // SQLCipher for encryption
    implementation(libs.sqlcipher)
    implementation(libs.androidx.sqlite)

    // Dependency Injection (Hilt)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler) // Changed from kapt to ksp

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // ViewModel
    implementation(libs.androidx.lifecycle.runtime.ktx) // Your existing version (e.g., 2.7.0)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Biometric Authentication
    implementation(libs.androidx.biometric)

    // Security
    implementation(libs.androidx.security.crypto)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}