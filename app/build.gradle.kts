plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.pixvi"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.pixvi"
        minSdk = 31
        targetSdk = 36 //new version haven't tested to see if all requirements are fulfilled
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            //for R8 build
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    kotlin {
        compilerOptions {
            jvmToolchain(11)
        }
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "META-INF/INDEX.LIST"
        }
    }
}

dependencies {
    // Core & UI

    implementation(libs.androidx.core.splashscreen)

    implementation (libs.androidx.work.runtime.ktx)
    // Room dependencies
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.media3.session)
    add("ksp", libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.material)

    // Lifecycle components
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Navigation
    implementation(libs.navigation.compose)
    implementation(libs.accompanistNavigationAnimation)

    // Networking - Retrofit & OkHttp
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)

    // Networking - Ktor (Why I have both  Retrofit and Ktor, lazy to check right now)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.serialization.jvm)
    implementation(libs.ktor.serialization.kotlinx.json) // For Ktor with Kotlinx Serialization
    implementation(libs.ktor.client.logging.jvm)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.cio)
    implementation(libs.logback.classic) // For Ktor logging

    // Data & Serialization
    implementation(libs.gson)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)

    // Security
    implementation(libs.androidx.security.crypto)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Browser
    implementation(libs.androidx.browser)

    // Image Loading and animated images
    implementation(libs.coil.compose)
    implementation("io.coil-kt:coil-gif:2.7.0") //libs.coil.gif is casing an error...

    //Extended Icons
    implementation(libs.androidx.material.icons.extended)

    implementation("androidx.media3:media3-exoplayer:1.7.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.7.1")
    implementation("androidx.media3:media3-ui:1.7.1")
    implementation("androidx.media3:media3-ui-compose:1.7.1")

    //temp
    implementation(libs.logging.interceptor)


    implementation (libs.androidx.constraintlayout.compose)
    implementation(libs.androidx.palette) // Palette API for color extraction
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.androidx.animation)

    // Parsing
    implementation(libs.jsoup)
    implementation(libs.protolite.well.known.types)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
