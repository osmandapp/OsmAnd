
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
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
            linkerOpts.add("-lsqlite3")
        }
    }

    val sqliteVersion = "2.3.1"
    val serializationVersion = "1.6.3"
    val coroutinesCoreVersion = "1.8.1"
    val datetimeVersion = "0.6.0"
    val okioVersion = "3.9.0"
    val kxml2Version = "2.1.8"
    val sqliterVersion = "1.3.1"

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlin:kotlin-stdlib")
            implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
            implementation ("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesCoreVersion")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:$datetimeVersion")
            implementation("com.squareup.okio:okio:$okioVersion")
            implementation("co.touchlab:sqliter-driver:$sqliterVersion")
        }
        androidMain.dependencies {
            implementation("androidx.sqlite:sqlite:$sqliteVersion")
            implementation("androidx.sqlite:sqlite-framework:$sqliteVersion")
            implementation("net.sf.kxml:kxml2:$kxml2Version")
        }
        iosMain.dependencies {
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
