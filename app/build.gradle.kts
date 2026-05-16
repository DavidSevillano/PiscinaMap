import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics.plugin)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.burixer85.piscinamap"
    compileSdk {
        version = release(36)
    }

    signingConfigs {
        create("release") {
            storeFile = localProperties.getProperty("keystore.path")?.let { file(it) }
            storePassword = localProperties.getProperty("keystore.password")
            keyAlias = localProperties.getProperty("key.alias")
            keyPassword = localProperties.getProperty("key.password")
        }
    }

    defaultConfig {
        applicationId = "com.burixer85.piscinamap"
        minSdk = 26
        targetSdk = 36
        versionCode = 6
        versionName = "1.0.4"

        testInstrumentationRunner = "com.burixer85.piscinamap.HiltTestRunner"

        buildConfigField(
            "String",
            "GOOGLEMAPS_KEY",
            "\"${localProperties.getProperty("googlemaps.key")}\""
        )

        buildConfigField(
            "String",
            "TEST_DEVICE_ID",
            "\"${localProperties.getProperty("admob.test.device.id")}\""
        )

        buildConfigField(
            "String",
            "ADMOB_INTERSTITIAL_ID",
            "\"${localProperties.getProperty("admob.interstitial.id")}\""
        )

        buildConfigField(
            "String",
            "ADMOB_BANNER_ID",
            "\"${localProperties.getProperty("admob.banner.id")}\""
        )

        buildConfigField(
            "String",
            "ADMOB_NATIVE_ID",
            "\"${localProperties.getProperty("admob.native.id")}\""
        )
        buildConfigField(
            "Boolean",
            "USE_TEST_ADS",
            "false"
        )

        manifestPlaceholders["googlemapsKey"] = "${localProperties.getProperty("googlemaps.key")}"
        manifestPlaceholders["admobAppId"] = "${localProperties.getProperty("admob.app.id")}"

    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "TEST_DEVICE_ID",
                "\"${localProperties.getProperty("admob.test.device.id")}\""
            )
        }
        release {
            // 2. Asignar la firma a la variante de release
            signingConfig = signingConfigs.getByName("release")

            isMinifyEnabled = false // Cámbialo a true cuando vayas a subir a producción
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            buildConfigField("String", "TEST_DEVICE_ID", "\"\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.runtime)
    implementation(libs.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.play.services.ads)

    // Google Maps
    implementation(libs.google.maps.android.maps.compose)
    implementation(libs.google.android.gms.play.services.maps)

    // Localización (GPS)
    implementation(libs.google.android.gms.play.services.location)

    // Networking
    implementation(libs.squareup.retrofit2.retrofit)
    implementation(libs.jakewharton.retrofit2.kotlinx.serialization.converter)
    implementation(libs.jetbrains.kotlinx.kotlinx.serialization.json)

    // OkHttp
    implementation(libs.squareup.okhttp3.logging.interceptor)

    // Tests
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.squareup.okhttp3.mockwebserver)

    // ViewModel & Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // JSON & Data
    implementation(libs.gson)
    implementation(libs.kotlinx.serialization.core)

    // Navegación 3
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Motor de red necesario
    implementation(libs.ktor.client.android)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation(libs.kotlinx.coroutines.test)

    // Splash Screen
    implementation(libs.androidx.core.splashscreen)

    // UI Components
    implementation(libs.google.android.material)
    implementation(libs.google.accompanist.permissions)
    implementation(libs.google.android.gms.play.services.location)
    implementation(libs.androidx.compose.material.icons.extended)

    // Google Places
    implementation(libs.google.android.libraries.places)

    // Coil
    implementation(libs.coil.compose)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
}


