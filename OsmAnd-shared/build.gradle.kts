import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	id("org.jetbrains.kotlin.multiplatform")
	id("org.jetbrains.kotlin.plugin.serialization")
	id("com.android.library")
	id("maven-publish")
	id("ivy-publish")
}

group = "net.osmand.shared"

kotlin {
	jvm {
		@OptIn(ExperimentalKotlinGradlePluginApi::class)
		compilerOptions {
			jvmTarget.set(JvmTarget.JVM_17)
		}
	}

	androidTarget {
		@OptIn(ExperimentalKotlinGradlePluginApi::class)
		compilerOptions {
			jvmTarget.set(JvmTarget.JVM_17)
		}
		publishLibraryVariants("release", "debug")
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
	val serializationVersion = "1.6.3"
	val coroutinesCoreVersion = "1.8.1"
	val datetimeVersion = "0.6.0"
	val okioVersion = "3.9.0"
	val kxml2Version = "2.3.0"
	val sqliterVersion = "1.3.1"
	val sqliteJDBCVersion = "3.34.0"
	val commonLoggingVersion = "1.2"

	sourceSets {
		commonMain.dependencies {
			implementation("org.jetbrains.kotlin:kotlin-stdlib")
			implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
			implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
			implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesCoreVersion")
			implementation("org.jetbrains.kotlinx:kotlinx-datetime:$datetimeVersion")
			implementation("com.squareup.okio:okio:$okioVersion")
		}
		jvmMain.dependencies {
			//implementation(kotlin("stdlib-jdk8"))
			implementation("net.sf.kxml:kxml2:$kxml2Version")
			implementation("org.xerial:sqlite-jdbc:$sqliteJDBCVersion")
			implementation("commons-logging:commons-logging:$commonLoggingVersion")
		}
		androidMain.dependencies {
			implementation("androidx.sqlite:sqlite:$sqliteVersion")
			implementation("androidx.sqlite:sqlite-framework:$sqliteVersion")
			implementation("net.sf.kxml:kxml2:$kxml2Version")
		}
		iosMain.dependencies {
			implementation("co.touchlab:sqliter-driver:$sqliterVersion")
		}
	}
}

android {
	namespace = "net.osmand.shared"
	compileSdk = 34
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}
	defaultConfig {
		minSdk = 24
	}
}

version = System.getenv("OSMAND_SHARED_ANDROID_BINARIES_IVY_REVISION") ?: "master-snapshot"
publishing {
	repositories {
		ivy {
			url = uri(System.getenv("OSMAND_BINARIES_IVY_ROOT") ?: "./")
		}
	}
	publications {
		create<IvyPublication>("ivyOsmAndSharedAndroid") {
			organisation = "net.osmand.shared"
			module = "OsmAnd-shared-android"
			revision = "$version"
			artifact(file("build/outputs/aar/OsmAnd-shared-debug.aar")) {
				type = "aar"
				classifier = "debug"
			}
			artifact(file("build/outputs/aar/OsmAnd-shared-release.aar")) {
				type = "aar"
				classifier = "release"
			}
		}
	}
}

tasks.named("publishIvyOsmAndSharedAndroidPublicationToIvyRepository") {
	dependsOn("bundleDebugAar", "bundleReleaseAar")
}