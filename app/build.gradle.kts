import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// API: pos.api.url (dirección completa, por ejemplo un túnel) o pos.api.puerto
// (puerto local) en local.properties
val propiedadesLocales = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }
        ?.inputStream()?.use { load(it) }
}

android {
    namespace = "com.example.myapplication2"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.myapplication2"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "API_PUERTO",
            "\"${propiedadesLocales.getProperty("pos.api.puerto", "3000")}\""
        )

        buildConfigField(
            "String",
            "API_URL",
            "\"${propiedadesLocales.getProperty("pos.api.url", "")}\""
        )
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // Conexión con la API
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    //ejecutar peticiones secgun diclo de vida de la pantalla
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}