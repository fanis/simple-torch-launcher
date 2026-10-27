import java.util.Properties

plugins {
    id("com.android.application")
}

// Local release signing, set up as in the owner's other Android apps: keystore.properties at the
// repo root names a keystore under release/, and both are kept out of git. Without them, as on
// GitHub Actions and F-Droid, the release APK stays unsigned and gets signed afterwards.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun releaseSigningValue(name: String): String? =
    keystoreProperties.getProperty(name) ?: findProperty(name)?.toString() ?: System.getenv(name)

val releaseStoreFile = releaseSigningValue("RELEASE_STORE_FILE")
val releaseStorePassword = releaseSigningValue("RELEASE_STORE_PASSWORD")
val releaseKeyAlias = releaseSigningValue("RELEASE_KEY_ALIAS")
val releaseKeyPassword = releaseSigningValue("RELEASE_KEY_PASSWORD")
val releaseSigningConfigured = listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword)
    .all { !it.isNullOrBlank() }

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

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
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
