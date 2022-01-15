/*
 * Copyright (C) 2015-2016 Daniel Schaal <daniel@schaal.email>
 *
 * This file is part of OCReader.
 *
 * OCReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OCReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OCReader.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-parcelize")
    id("kotlin-kapt")
    id("realm-android")
    id("com.mikepenz.aboutlibraries.plugin")
}

android {
    compileSdkVersion(30)
    buildToolsVersion = "30.0.3"

    buildFeatures.dataBinding = true

    defaultConfig {
        applicationId = "email.schaal.ocreader"
        minSdkVersion(23)
        targetSdkVersion(30)
        versionCode = 58
        versionName = "0.58"

        base.archivesName.set("${applicationId}_${versionCode}")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            applicationIdSuffix = ".debug"
        }
        getByName("release") {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    lintOptions {
        isAbortOnError = true
        disable.add("MissingTranslation")
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

object Versions {
    const val okhttp     = "4.9.1"
    const val retrofit   = "2.9.0"
    const val glide      = "4.12.0"
    const val lifecycle  = "2.3.1"
    const val moshi      = "1.12.0"
    const val core       = "1.6.0"
    const val annotation = "1.2.0"
    const val junit_ext  = "1.1.3"
    const val espresso   = "3.4.0"
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.21")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1")

    implementation("androidx.core:core-ktx:${Versions.core}")
    implementation("androidx.core:core-ktx:${Versions.core}")

    implementation("androidx.appcompat:appcompat:1.3.1")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.lifecycle}")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:${Versions.lifecycle}")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:${Versions.lifecycle}")
    implementation("androidx.lifecycle:lifecycle-common-java8:${Versions.lifecycle}")

    implementation("androidx.work:work-runtime-ktx:2.5.0")
    implementation("androidx.fragment:fragment-ktx:1.3.6")
    implementation("androidx.activity:activity-ktx:1.2.4")

    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.preference:preference-ktx:1.1.1")
    implementation("androidx.viewpager2:viewpager2:1.1.0-alpha01")
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")

    implementation("androidx.annotation:annotation:${Versions.annotation}")
    kapt("androidx.annotation:annotation:${Versions.annotation}")

    implementation("com.google.android.material:material:1.4.0")

    implementation("com.mikepenz:aboutlibraries:8.9.1")

    implementation("org.jsoup:jsoup:1.14.1")

    implementation("com.squareup.okhttp3:okhttp:${Versions.okhttp}")

    implementation("com.squareup.retrofit2:retrofit:${Versions.retrofit}")
    implementation("com.squareup.retrofit2:converter-moshi:${Versions.retrofit}")

    implementation("com.squareup.moshi:moshi:${Versions.moshi}")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:${Versions.moshi}")

    implementation("com.github.bumptech.glide:glide:${Versions.glide}")
    kapt("com.github.bumptech.glide:compiler:${Versions.glide}")

    implementation("com.github.bumptech.glide:okhttp3-integration:${Versions.glide}")
    implementation("com.github.bumptech.glide:recyclerview-integration:${Versions.glide}")

    implementation("com.github.zafarkhaja:java-semver:0.9.0")

    androidTestImplementation("androidx.test.ext:junit:${Versions.junit_ext}")
    androidTestImplementation("androidx.test.espresso:espresso-core:${Versions.espresso}")
    androidTestImplementation("androidx.test.espresso:espresso-intents:${Versions.espresso}")

    androidTestImplementation("com.squareup.okhttp3:mockwebserver:${Versions.okhttp}")

    testImplementation("androidx.test:core:1.4.0")
    testImplementation("androidx.test.ext:junit:${Versions.junit_ext}")
    testImplementation("org.robolectric:robolectric:4.6.1")
    testImplementation("junit:junit:4.13.2")
}
