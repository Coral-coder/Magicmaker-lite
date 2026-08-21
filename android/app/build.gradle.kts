plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.magicmaker.lite"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.magicmaker.lite"
        minSdk = 26
        targetSdk = 34
        // Overridable from CI so each build can carry its own version:
        //   ./gradlew assembleRelease -PmmVersionCode=7 -PmmVersionName=1.0.7
        versionCode = (project.findProperty("mmVersionCode") as String?)?.toInt() ?: 1
        versionName = (project.findProperty("mmVersionName") as String?) ?: "1.0.0"
    }

    // A release APK is only installable if it is signed. The key never lives in the repo — it is
    // supplied through env vars (CI secrets / a local keystore), and without them the release
    // build stays unsigned and CI publishes the debug APK instead.
    signingConfigs {
        create("release") {
            val storePath = System.getenv("MM_KEYSTORE_FILE")
            if (!storePath.isNullOrBlank() && file(storePath).exists()) {
                storeFile = file(storePath)
                storePassword = System.getenv("MM_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("MM_KEY_ALIAS")
                keyPassword = System.getenv("MM_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (!System.getenv("MM_KEYSTORE_FILE").isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
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
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
