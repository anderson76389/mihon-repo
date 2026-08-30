plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "eu.kanade.tachiyomi.extension.fr"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            storeFile = file("${project.rootDir}/release.keystore")
            storePassword = "UniversalKey2026!"
            keyAlias = "mihonkey"
            keyPassword = "UniversalKey2026!"
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    flavorDimensions += "source"
    productFlavors {
        create("mangasorigines") {
            dimension = "source"
            applicationId = "eu.kanade.tachiyomi.extension.fr.mangasorigines"
            manifestPlaceholders["extClass"] = "eu.kanade.tachiyomi.extension.fr.MangasOrigines"
            manifestPlaceholders["appName"] = "Mangas Origines"
        }
        create("sushiscan") {
            dimension = "source"
            applicationId = "eu.kanade.tachiyomi.extension.fr.sushiscan"
            manifestPlaceholders["extClass"] = "eu.kanade.tachiyomi.extension.fr.SushiScan"
            manifestPlaceholders["appName"] = "Sushi-Scan"
        }
        create("scanmanga") {
            dimension = "source"
            applicationId = "eu.kanade.tachiyomi.extension.fr.scanmanga"
            manifestPlaceholders["extClass"] = "eu.kanade.tachiyomi.extension.fr.ScanManga"
            manifestPlaceholders["appName"] = "Scan-Manga"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
