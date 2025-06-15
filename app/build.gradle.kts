plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.cypher.zealth"
    compileSdk = 36
    ndkVersion = "28.0.13004108"


    defaultConfig {
        applicationId = "com.cypher.zealth"
        minSdk = 24
        targetSdk = 36
        versionCode = 17
        versionName = "3.2.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Optional: keep only arm64 if you want smaller APK/AAB
        ndk {
            abiFilters += setOf("arm64-v8a")
        }
    }

    signingConfigs {
        create("release") {
            val storeFilePath = project.findProperty("ZEALTH_STORE_FILE") as String?
                ?: throw GradleException("Missing ZEALTH_STORE_FILE in gradle.properties")
            storeFile = file(storeFilePath)

            storePassword = project.findProperty("ZEALTH_STORE_PASSWORD") as String?
                ?: throw GradleException("Missing ZEALTH_STORE_PASSWORD in gradle.properties")

            keyAlias = project.findProperty("ZEALTH_KEY_ALIAS") as String?
                ?: throw GradleException("Missing ZEALTH_KEY_ALIAS in gradle.properties")

            keyPassword = project.findProperty("ZEALTH_KEY_PASSWORD") as String?
                ?: throw GradleException("Missing ZEALTH_KEY_PASSWORD in gradle.properties")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")

            // Deobfuscation file (mapping.txt)
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // Native debug symbols (for .so from dependencies too)
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE" // or "FULL"
            }
        }

        debug {
            // defaults
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // Removes the "jvmTarget is deprecated" warning (new DSL)
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1"
            )
        }
    }
}

dependencies {
    // AndroidX / UI
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.material)

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // JSON
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("org.json:json:20240303")

    // Maps
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation("org.osmdroid:osmdroid-mapsforge:6.1.18")
    implementation("com.github.MKergall:osmbonuspack:6.9.0")

    // Utilities
    implementation("androidx.collection:collection:1.4.0")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    // Media3
    implementation("androidx.media3:media3-exoplayer:1.7.0")
    implementation("androidx.media3:media3-ui:1.7.0")

    // BlurView (keep ONE)
    implementation("com.github.Dimezis:BlurView:version-3.2.0")

    // SceneView
    implementation("io.github.sceneview:sceneview:2.3.3")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Splash Screen
    implementation("androidx.core:core-splashscreen:1.0.1")

    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    implementation( "com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor( "com.github.bumptech.glide:compiler:4.16.0")


    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
