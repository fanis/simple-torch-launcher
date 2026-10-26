plugins {
    id("com.android.application")
}

android {
    namespace = "dev.fanis.simpletorchlauncher"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.fanis.simpletorchlauncher"
        minSdk = 23
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    buildFeatures {
        buildConfig = true
    }

    // F-Droid rejects the dependency metadata blob that AGP otherwise embeds, signed with a Google key.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}
