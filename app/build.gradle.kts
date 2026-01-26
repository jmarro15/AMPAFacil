plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    //Alias para conexion con firebase, me fallaba y no sabia por que era este libreria habia que ponerla en tre sitios en la raiz, en libr.version y aqui.
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.ampafacil.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ampafacil.app"
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

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    //Navegacion con compose
    implementation(libs.androidx.navigation.compose)

    // --- Firebase BoM: fija versiones compatibles entre librerías Firebase (evita líos de versiones) ---
    implementation(platform("com.google.firebase:firebase-bom:34.8.0"))

    // --- Firebase Auth: habilita login/registro con email y contraseña (y más métodos si luego quieres) ---
    implementation("com.google.firebase:firebase-auth")



}