plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    // REQUIRED on Kotlin 2.x: since Kotlin 2.0 the Compose compiler ships as a
    // Gradle plugin. Never add `composeOptions { kotlinCompilerExtensionVersion }`
    // here — on Kotlin 2.x that hard-fails configuration.
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.aegis.vault"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aegis.vault"
        minSdk = 26
        targetSdk = 35
        versionCode = 420
        versionName = "4.2.0"

        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro",
        )
    }

    buildTypes {
        // The published asset is the DEBUG apk, and CI runs only `assembleDebug`.
        // No applicationIdSuffix on purpose, so the output path stays exactly
        // app/build/outputs/apk/debug/app-debug.apk for the nightly release job.
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            // Native-library AARs (SQLCipher) and their transitive jars commonly
            // collide on these Android-arsenal boilerplate manifest files.
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/*.SF"
            excludes += "/META-INF/*.DSA"
            excludes += "/META-INF/*.RSA"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)

    // BOM first: `platform()`, NOT `implementation()`.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.animation)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.biometric)
    implementation(libs.androidx.sqlite)
    implementation(libs.androidx.security.crypto)
    implementation(libs.sqlcipher.android)
}
