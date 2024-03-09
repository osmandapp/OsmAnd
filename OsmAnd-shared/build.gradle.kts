
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "OsmAndShared"
            isStatic = true
        }
    }

    val sqliteVersion = "2.3.1"
    val sqlDelightVersion = "1.5.4"

    sourceSets {
        commonMain.dependencies {
            implementation("com.squareup.sqldelight:runtime:$sqlDelightVersion")
        }
        androidMain.dependencies {
            implementation("com.squareup.sqldelight:android-driver:$sqlDelightVersion")
            implementation("androidx.sqlite:sqlite-framework:$sqliteVersion")
        }
        iosMain.dependencies {
            implementation("com.squareup.sqldelight:native-driver:$sqlDelightVersion")
        }
    }
}

android {
    namespace = "net.osmand.shared"
    compileSdk = 33
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = 23
    }
}
