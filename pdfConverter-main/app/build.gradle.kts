plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.pdfconverter"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.pdfconverter"
        minSdk = 26
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

    implementation ("androidx.activity:activity-compose:1.7.2")
    implementation ("androidx.compose.ui:ui:1.8.3")
    implementation ("androidx.compose.material:material:1.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")


    //for getting file name from internal storage
    implementation ("androidx.documentfile:documentfile:1.1.0")

    //for reading file data
    implementation("com.itextpdf:itextpdf:5.5.13.4")


    //for coroutine
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    //for docx
    implementation("org.apache.poi:poi-ooxml:5.2.3")
    implementation("org.apache.poi:poi-scratchpad:5.2.3")
    
    //navigation
        implementation("androidx.navigation:navigation-compose:2.9.1")

    //for icons
    implementation ("androidx.compose.material:material-icons-extended:1.7.8")

    //for viewing pdf
    implementation("com.google.accompanist:accompanist-pager:0.34.0")


}