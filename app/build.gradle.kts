plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.perkapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.perkapp"
        minSdk = 24
        targetSdk = 35
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
// Hilt core
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1") // ganti kapt dengan ksp
    // Hilt + Navigation Compose (untuk hiltViewModel())
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")


    // Untuk icon tambahan seperti Sync, CloudUpload, EventNote, dan icon Compose lainnya.
    implementation("androidx.compose.material:material-icons-extended")

////Coil itu library untuk menampilkan gambar dari: URL internet,API, database ,link foto profile
    implementation("io.coil-kt:coil-compose:2.6.0")

// Untuk menghubungkan lifecycle Android dengan Jetpack Compose.
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

// Untuk menggunakan ViewModel langsung di Jetpack Compose.
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

// Untuk navigasi antar halaman/screen di Jetpack Compose.
    implementation("androidx.navigation:navigation-compose:2.7.7")

// Untuk menyimpan data sederhana seperti token login dan session user.
    implementation("androidx.datastore:datastore-preferences:1.0.0")

// Untuk menjalankan proses asynchronous/background di Kotlin.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

// Untuk melakukan request API ke backend/server.
    implementation("com.squareup.retrofit2:retrofit:2.9.0")

// Untuk mengubah JSON API menjadi object Kotlin.
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

// Untuk HTTP client yang digunakan oleh Retrofit.
    implementation("com.squareup.okhttp3:okhttp:4")



    // 1. Jetpack Navigation Compose (Untuk NavGraph & Routes)
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // 2. Preferences DataStore (Untuk simpan token)
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    // 3. Retrofit & OkHttp (Untuk API & Auth Interceptor)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    // ViewModel Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")


    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.room3.external.antlr)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Coroutines
    implementation(libs.coroutines.android)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.compose.material.icons.extended)

    // WorkManager for offline sync
    implementation(libs.work.runtime.ktx)
}