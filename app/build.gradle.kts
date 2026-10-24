plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

import java.io.FileInputStream
import java.util.Properties

val releaseKeystoreProperties = Properties().apply {
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    if (keystorePropertiesFile.exists()) {
        FileInputStream(keystorePropertiesFile).use { inputStream ->
            load(inputStream)
        }
    }
}

val hasReleaseKeystore = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
    .all { releaseKeystoreProperties.getProperty(it).isNullOrBlank().not() }

val requestingReleaseBuild = gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }

if (requestingReleaseBuild && !hasReleaseKeystore) {
    error("Missing keystore.properties. Create it from keystore.properties.example before building release artifacts.")
}

android {
    namespace = "com.kushal.xdate"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.kushal.xdate"
        minSdk = 24
        targetSdk = 34
        versionCode = 3
        versionName = "1.2"
    }

    if (hasReleaseKeystore) {
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(releaseKeystoreProperties.getProperty("storeFile"))
                storePassword = releaseKeystoreProperties.getProperty("storePassword")
                keyAlias = releaseKeystoreProperties.getProperty("keyAlias")
                keyPassword = releaseKeystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".share"
            versionNameSuffix = "-share"
            resValue("string", "app_name", "XDate Share")
        }

        release {
            if (hasReleaseKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // Keep the APK tiny: no Kotlin metadata or dependency blobs are needed at runtime.
    packaging {
        resources.excludes += listOf("kotlin/**", "META-INF/*.version", "META-INF/*.kotlin_module", "DebugProbesKt.bin")
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
