plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.cryptic.pixvi"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.cryptic.pixvi"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    @Suppress("DEPRECATION")
    defaultConfig {
        renderscriptTargetApi = 21
        renderscriptSupportModeEnabled = true
    }
}

dependencies {
    //brower
    implementation(libs.androidx.browser)
    //retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.kotlinx.serialization)
    implementation(libs.converter.scalars)
    //coil
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    //room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    //serliasation
    implementation(libs.kotlinx.serialization.json)

    //
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    //tink
    implementation (libs.tink.android)

    //datastore
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.material.icons.extended)

    //navigation 3
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    //coil
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")
    implementation("io.coil-kt.coil3:coil-gif:3.3.0")
    implementation("com.github.Commit451.coil-transformations:transformations:3.0.0")

    //work manager
    implementation(libs.androidx.work.runtime.ktx)



    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}