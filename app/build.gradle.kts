plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "id.aruna.domino"
    compileSdk = 35

    defaultConfig {
        applicationId = "id.aruna.domino"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    sourceSets["main"].java.srcDirs(
        "src/main/kotlin",
        "../core/src/main/kotlin"
    )

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
