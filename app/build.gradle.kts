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

    flavorDimensions += "ext"
    productFlavors {
        create("mangasorigines") {
            dimension = "ext"
            applicationId = "eu.kanade.tachiyomi.extension.fr.mangasorigines"
            manifestPlaceholders["appName"] = "Mangas Origines"
            manifestPlaceholders["extClass"] = "eu.kanade.tachiyomi.extension.fr.MangasOrigines"
        }
        create("sushiscan") {
            dimension = "ext"
            applicationId = "eu.kanade.tachiyomi.extension.fr.sushiscan"
            manifestPlaceholders["appName"] = "Sushi-Scan"
            manifestPlaceholders["extClass"] = "eu.kanade.tachiyomi.extension.fr.SushiScan"
        }
        create("scanmanga") {
            dimension = "ext"
            applicationId = "eu.kanade.tachiyomi.extension.fr.scanmanga"
            manifestPlaceholders["appName"] = "Scan-Manga"
            manifestPlaceholders["extClass"] = "eu.kanade.tachiyomi.extension.fr.ScanManga"
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
