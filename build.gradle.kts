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

buildscript {
    repositories {
        mavenCentral()
        google()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.2.2")
        // keep kotlin version in sync with app/build.gradle.kts
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.21")
        classpath("io.realm:realm-gradle-plugin:10.6.1")
    }
}

plugins {
    // keep aboutlibraries version in sync with app/build.gradle.kts
    id("com.mikepenz.aboutlibraries.plugin") version "8.9.0" apply false
}

allprojects {
    repositories {
        mavenCentral()
        google()
    }
}

tasks.register("clean",  Delete::class)  {
    delete(rootProject.buildDir)
}